package com.example.biodontos.presentation.patientportal.appointments

import app.cash.turbine.test
import arrow.core.right
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Appointment
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BookAppointmentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

    private val testSlot = TimeSlot(
        start = testNow.plusHours(3),
        end = testNow.plusHours(3).plusMinutes(30)
    )

    @Before
    fun setUp() {
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns testNow
        coEvery { getCurrentPatientUseCase() } returns Result.success(testPatient)
        coEvery { doctorRepository.getDoctorById("doc-1") } returns Result.success(testDoctor)
        coEvery { getAvailableSlotsUseCase(any(), any()) } returns Result.success(listOf(testSlot))
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

    @Test
    fun `initial load populates doctor and available slots`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.doctor)
        assertEquals("Dr. Carolina Duque", state.doctor?.displayName)
        assertEquals(1, state.slots.size)
        assertEquals(14, state.availableDates.size)
    }

    @Test
    fun `select date loads slots for selected date`() = runTest {
        val nextDay = testToday.plusDays(1)
        val nextDaySlot = TimeSlot(
            start = nextDay.atTime(10, 0),
            end = nextDay.atTime(10, 30)
        )
        coEvery { getAvailableSlotsUseCase(DoctorId("doc-1"), nextDay) } returns Result.success(listOf(nextDaySlot))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectDate(nextDay))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(nextDay, state.selectedDate)
        assertEquals(1, state.slots.size)
        assertEquals(nextDaySlot, state.slots[0])
    }

    @Test
    fun `confirm booking successfully books and emits NavigateToConfirmation`() = runTest {
        val testAppointment = Appointment(
            id = "appt-99",
            patientId = "patient-1",
            doctorId = "doc-1",
            startTime = testSlot.start,
            endTime = testSlot.end
        )
        coEvery {
            bookAppointmentUseCase(
                patientId = "patient-1",
                doctorId = "doc-1",
                startTime = testSlot.start,
                endTime = testSlot.end,
                reason = "Control de rutina"
            )
        } returns Result.success(testAppointment)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(testSlot))

        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
            val toast = awaitItem()
            assert(toast is BookAppointmentUiEffect.ShowToast)
            val navigate = awaitItem()
            assert(navigate is BookAppointmentUiEffect.NavigateToConfirmation)
            val confirmation = (navigate as BookAppointmentUiEffect.NavigateToConfirmation).confirmation
            assertEquals("appt-99", confirmation.appointmentId)
            assertEquals("Dr. Carolina Duque", confirmation.doctorName)
            assertEquals("Ortodoncia", confirmation.specialty)
            assertEquals(testSlot.start.toString(), confirmation.startIso)
            assertEquals("Control de rutina", confirmation.reason)
            assertFalse(confirmation.isRescheduled)
        }
    }

    @Test
    fun `confirm booking when rescheduling triggers 2-step confirmation dialog`() = runTest {
        val viewModel = createViewModel(rescheduleId = "appt-existing")
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(testSlot))
        assertFalse(viewModel.uiState.value.showRescheduleConfirmDialog)

        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        assertTrue(viewModel.uiState.value.showRescheduleConfirmDialog)
    }

    @Test
    fun `dismiss reschedule dialog resets dialog flag`() = runTest {
        val viewModel = createViewModel(rescheduleId = "appt-existing")
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(testSlot))
        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        assertTrue(viewModel.uiState.value.showRescheduleConfirmDialog)

        viewModel.onEvent(BookAppointmentUiEvent.DismissRescheduleDialog)
        assertFalse(viewModel.uiState.value.showRescheduleConfirmDialog)
    }

    @Test
    fun `execute reschedule completes and emits NavigateToConfirmation`() = runTest {
        coEvery {
            rescheduleAppointmentUseCase(
                appointmentId = "appt-existing",
                newStartTime = testSlot.start,
                newEndTime = testSlot.end
            )
        } returns Unit.right()

        val viewModel = createViewModel(rescheduleId = "appt-existing")
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(testSlot))
        viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
        assertTrue(viewModel.uiState.value.showRescheduleConfirmDialog)

        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ExecuteReschedule)
            val toast = awaitItem()
            assert(toast is BookAppointmentUiEffect.ShowToast)
            val navigate = awaitItem()
            assert(navigate is BookAppointmentUiEffect.NavigateToConfirmation)
            val confirmation = (navigate as BookAppointmentUiEffect.NavigateToConfirmation).confirmation
            assertEquals("appt-existing", confirmation.appointmentId)
            assertEquals("Dr. Carolina Duque", confirmation.doctorName)
            assertTrue(confirmation.isRescheduled)
        }
        assertFalse(viewModel.uiState.value.showRescheduleConfirmDialog)
    }

    @Test
    fun `slot load failure updates errorMessage and allows retry`() = runTest {
        coEvery { getAvailableSlotsUseCase(DoctorId("doc-1"), any()) } returns Result.failure(RuntimeException("Error de conexión"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSlotsLoading)
        assertEquals("No se pudieron consultar los horarios disponibles. Intenta nuevamente.", state.errorMessage)
        assertEquals(0, state.slots.size)

        coEvery { getAvailableSlotsUseCase(DoctorId("doc-1"), any()) } returns Result.success(listOf(testSlot))
        viewModel.onEvent(BookAppointmentUiEvent.RetrySlots)
        advanceUntilIdle()

        val recoveredState = viewModel.uiState.value
        assertEquals(null, recoveredState.errorMessage)
        assertEquals(1, recoveredState.slots.size)
    }

    @Test
    fun `confirm booking with slot less than 2 hours emits past slot toast`() = runTest {
        val pastSlot = TimeSlot(
            start = testNow.plusMinutes(60),
            end = testNow.plusMinutes(90)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(BookAppointmentUiEvent.SelectSlot(pastSlot))

        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
            val toast = awaitItem()
            assert(toast is BookAppointmentUiEffect.ShowToast)
            assertEquals("El horario seleccionado ya no está disponible (mínimo 2 horas de anticipación)", (toast as BookAppointmentUiEffect.ShowToast).message)
        }
    }
}

