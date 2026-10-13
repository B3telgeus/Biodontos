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
    fun `confirm booking successfully books and navigates to my appointments`() = runTest {
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
            assertEquals(BookAppointmentUiEffect.NavigateToMyAppointments, navigate)
        }
    }

    @Test
    fun `confirm booking with rescheduleId calls reschedule and navigates`() = runTest {
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

        viewModel.effects.test {
            viewModel.onEvent(BookAppointmentUiEvent.ConfirmBooking)
            val toast = awaitItem()
            assert(toast is BookAppointmentUiEffect.ShowToast)
            val navigate = awaitItem()
            assertEquals(BookAppointmentUiEffect.NavigateToMyAppointments, navigate)
        }
    }
}
