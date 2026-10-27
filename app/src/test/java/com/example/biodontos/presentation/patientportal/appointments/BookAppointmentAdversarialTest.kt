package com.example.biodontos.presentation.patientportal.appointments

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.scheduling.TimeSlot
import com.example.biodontos.domain.usecase.appointments.BookAppointmentUseCase
import com.example.biodontos.domain.usecase.appointments.GetAvailableSlotsUseCase
import com.example.biodontos.domain.usecase.appointments.RescheduleAppointmentUseCase
import com.example.biodontos.domain.usecase.patient.GetCurrentPatientUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BookAppointmentAdversarialTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val getCurrentPatientUseCase: GetCurrentPatientUseCase = mockk()
    private val doctorRepository: DoctorRepository = mockk()
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase = mockk()
    private val bookAppointmentUseCase: BookAppointmentUseCase = mockk()
    private val rescheduleAppointmentUseCase: RescheduleAppointmentUseCase = mockk()
    private val clinicClock: ClinicClock = mockk()

    private val testToday = LocalDate.of(2026, 10, 15)
    private val testNow = LocalDateTime.of(2026, 10, 15, 8, 0)

    private val testDoctor = Doctor(
        id = DoctorId("doc-1"),
        firstName = "Carolina",
        lastName = "Duque",
        specialty = "Ortodoncia",
        licenseNumber = "123456",
        email = "carolina@test.com"
    )

    private val testPatient = Patient(
        id = "patient-1",
        userId = "user-1",
        dniCedula = "10203040",
        firstName = "Daniel",
        firstLastName = "Gómez"
    )

    private val validFutureSlot = TimeSlot(
        start = testNow.plusHours(3),
        end = testNow.plusHours(3).plusMinutes(30)
    )

    @Before
    fun setUp() {
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns testNow
        coEvery { getCurrentPatientUseCase() } returns Result.success(testPatient)
        coEvery { doctorRepository.getDoctorById("doc-1") } returns Result.success(testDoctor)
        coEvery { getAvailableSlotsUseCase(any(), any()) } returns Result.success(listOf(validFutureSlot))
    }

    private fun createViewModel(
        doctorId: String = "doc-1",
        rescheduleId: String? = null
    ): BookAppointmentViewModel {
        return BookAppointmentViewModel(
            doctorId = doctorId,
            rescheduleAppointmentId = rescheduleId,
            getCurrentPatientUseCase = getCurrentPatientUseCase,
            doctorRepository = doctorRepository,
            getAvailableSlotsUseCase = getAvailableSlotsUseCase,
            bookAppointmentUseCase = bookAppointmentUseCase,
            rescheduleAppointmentUseCase = rescheduleAppointmentUseCase,
            clinicClock = clinicClock
        )
    }

    // =========================================================================
    // 1. RAPID BOOKING CLICKS & RE-ENTRANCY (CONCURRENCY STRESS)
    // =========================================================================

    @Test
    fun `rapid double click on ConfirmBooking should NOT invoke bookAppointmentUseCase twice`() = runTest(testDispatcher) {
        val bookingDeferred = CompletableDeferred<Result<Appointment>>()
        coEvery {
            bookAppointmentUseCase(any(), any(), any(), any(), any())
        } coAnswers {
            bookingDeferred.await()
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(validFutureSlot))

        // First click
        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        // Advance slightly so coroutine starts and sets isSubmitting
        testScheduler.advanceTimeBy(10)

        assertTrue(viewModel.uiState.value.isSubmitting)

        // Rapid second click while first is in-flight!
        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        testScheduler.advanceTimeBy(10)

        // Now complete the deferred
        val appt = Appointment(
            id = "appt-1",
            patientId = "patient-1",
            doctorId = "doc-1",
            startTime = validFutureSlot.start,
            endTime = validFutureSlot.end
        )
        bookingDeferred.complete(Result.success(appt))
        advanceUntilIdle()

        // EMPIRICAL VERIFICATION: Was bookAppointmentUseCase called 1 time or 2 times?
        coVerify(exactly = 1) {
            bookAppointmentUseCase(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `rapid double click on ExecuteReschedule should NOT invoke rescheduleAppointmentUseCase twice`() = runTest(testDispatcher) {
        val rescheduleDeferred = CompletableDeferred<arrow.core.Either<AppointmentError, Unit>>()
        coEvery {
            rescheduleAppointmentUseCase(any(), any(), any())
        } coAnswers {
            rescheduleDeferred.await()
        }

        val viewModel = createViewModel(rescheduleId = "appt-original")
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(validFutureSlot))
        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        assertTrue(viewModel.uiState.value.showRescheduleConfirmDialog)

        // First confirmation
        viewModel.onEvent(BookAppointmentUiEvent.ExecuteReschedule)
        testScheduler.advanceTimeBy(10)
        assertTrue(viewModel.uiState.value.isSubmitting)

        // Rapid second confirmation while in flight
        viewModel.onEvent(BookAppointmentUiEvent.ExecuteReschedule)
        testScheduler.advanceTimeBy(10)

        rescheduleDeferred.complete(Unit.right())
        advanceUntilIdle()

        // EMPIRICAL VERIFICATION
        coVerify(exactly = 1) {
            rescheduleAppointmentUseCase(any(), any(), any())
        }
    }

    // =========================================================================
    // 2. RESCHEDULE FLOW: CONFIRMING VS DISMISSING 2-STEP DIALOG
    // =========================================================================

    @Test
    fun `dismissing 2-step reschedule dialog PRESERVES old appointment and does NOT call use case`() = runTest(testDispatcher) {
        val viewModel = createViewModel(rescheduleId = "appt-original")
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(validFutureSlot))
        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        assertTrue("Dialog should be shown", viewModel.uiState.value.showRescheduleConfirmDialog)

        // Dismiss the dialog
        viewModel.onEvent(BookAppointmentUiEvent.DismissRescheduleDialog)

        // Dialog should be dismissed
        assertFalse(viewModel.uiState.value.showRescheduleConfirmDialog)
        assertFalse(viewModel.uiState.value.isSubmitting)
        // Selected slot is still in memory for patient convenience
        assertEquals(validFutureSlot, viewModel.uiState.value.selectedSlot)

        // Old appointment was untouched
        coVerify(exactly = 0) {
            rescheduleAppointmentUseCase(any(), any(), any())
        }
    }

    @Test
    fun `confirming 2-step reschedule dialog proceeds with reschedule and emits confirmation`() = runTest(testDispatcher) {
        coEvery {
            rescheduleAppointmentUseCase("appt-original", validFutureSlot.start, validFutureSlot.end)
        } returns Unit.right()

        val viewModel = createViewModel(rescheduleId = "appt-original")
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(validFutureSlot))
        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        assertTrue(viewModel.uiState.value.showRescheduleConfirmDialog)

        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ExecuteReschedule)
            advanceUntilIdle()

            val toast = awaitItem() as BookAppointmentUiEffect.ShowToast
            assertEquals("Cita reagendada con éxito", toast.message)

            val nav = awaitItem() as BookAppointmentUiEffect.NavigateToConfirmation
            assertEquals("appt-original", nav.confirmation.appointmentId)
            assertTrue("Should be marked as rescheduled", nav.confirmation.isRescheduled)
        }

        coVerify(exactly = 1) {
            rescheduleAppointmentUseCase("appt-original", validFutureSlot.start, validFutureSlot.end)
        }
    }

    // =========================================================================
    // 3. SLOT COLLISION ON BOOKING & RESCHEDULING
    // =========================================================================

    @Test
    fun `slot collision on new booking handles error gracefully and refreshes slots`() = runTest(testDispatcher) {
        coEvery {
            bookAppointmentUseCase(any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("Slot collision: horario ya ocupado"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(validFutureSlot))

        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
            advanceUntilIdle()

            val toast = awaitItem() as BookAppointmentUiEffect.ShowToast
            assertEquals("No se pudo agendar la cita. Intenta con otro horario.", toast.message)
        }

        assertFalse("Submitting should be reset to false", viewModel.uiState.value.isSubmitting)
        // Verify slots were reloaded for the date
        coVerify(atLeast = 2) {
            getAvailableSlotsUseCase(DoctorId("doc-1"), testToday)
        }
    }

    @Test
    fun `slot collision on reschedule handles SlotUnavailable and refreshes slots`() = runTest(testDispatcher) {
        coEvery {
            rescheduleAppointmentUseCase(any(), any(), any())
        } returns AppointmentError.SlotUnavailable.left()

        val viewModel = createViewModel(rescheduleId = "appt-original")
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(validFutureSlot))
        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        assertTrue(viewModel.uiState.value.showRescheduleConfirmDialog)

        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ExecuteReschedule)
            advanceUntilIdle()

            val toast = awaitItem() as BookAppointmentUiEffect.ShowToast
            assertEquals("El horario ya no está disponible o ha sido reservado.", toast.message)
        }

        assertFalse(viewModel.uiState.value.isSubmitting)
        coVerify(atLeast = 2) {
            getAvailableSlotsUseCase(DoctorId("doc-1"), testToday)
        }
    }

    // =========================================================================
    // 4. PAST DATE / SLOT BOUNDARY CONDITIONS
    // =========================================================================

    @Test
    fun `booking a slot exactly at 2 hours threshold vs 1 minute before threshold`() = runTest(testDispatcher) {
        // Slot at exactly 2h: testNow is 08:00, slot starts at 10:00 (now + 2h)
        val exact2hSlot = TimeSlot(
            start = testNow.plusHours(2),
            end = testNow.plusHours(2).plusMinutes(30)
        )
        // Slot at 1h 59m: starts at 09:59 (less than 2h)
        val justUnder2hSlot = TimeSlot(
            start = testNow.plusHours(2).minusMinutes(1),
            end = testNow.plusHours(2).plusMinutes(29)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 1) Test slot just under 2h -> Must be rejected
        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(justUnder2hSlot))
        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
            advanceUntilIdle()
            val toast = awaitItem() as BookAppointmentUiEffect.ShowToast
            assertTrue(toast.message.contains("mínimo 2 horas de anticipación"))
        }

        // Verify bookAppointmentUseCase was NOT called
        coVerify(exactly = 0) {
            bookAppointmentUseCase(any(), any(), any(), any(), any())
        }

        // 2) Test slot at exactly 2 hours -> Should be accepted (not rejected by 2h check)
        coEvery {
            bookAppointmentUseCase(any(), any(), any(), any(), any())
        } returns Result.success(
            Appointment(
                id = "appt-2",
                patientId = "patient-1",
                doctorId = "doc-1",
                startTime = exact2hSlot.start,
                endTime = exact2hSlot.end
            )
        )

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(exact2hSlot))
        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
            advanceUntilIdle()
            val toast = awaitItem() as BookAppointmentUiEffect.ShowToast
            assertEquals("¡Cita reservada con éxito!", toast.message)
            val nav = awaitItem() as BookAppointmentUiEffect.NavigateToConfirmation
            assertEquals("appt-2", nav.confirmation.appointmentId)
        }

        coVerify(exactly = 1) {
            bookAppointmentUseCase(any(), any(), any(), any(), any())
        }
    }
}
