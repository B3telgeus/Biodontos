package com.example.biodontos.presentation.doctor.appointments

import app.cash.turbine.test
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.ToggleAppointmentConfirmationUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorAppointmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase = mockk()
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase = mockk()
    private val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase = mockk()
    private val patientRepository: PatientRepository = mockk()
    private val clinicClock: ClinicClock = mockk()

    private val testDoctor = Doctor(
        id = DoctorId("doc-1"),
        firstName = "Andrés",
        lastName = "Castro",
        specialty = "Ortodoncia",
        licenseNumber = "12345",
        email = "andres@test.com"
    )

    private val testPatients = listOf(
        Patient(id = "pat-1", dniCedula = "111", firstName = "Luis", firstLastName = "Camilo")
    )

    private val todayDate = LocalDate.of(2026, 10, 20)

    private val testAppointments = listOf(
        Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = LocalDateTime.of(2026, 10, 20, 9, 0),
            isConfirmed = true,
            reason = "Control",
            status = AppointmentStatus.CONFIRMED
        ),
        Appointment(
            id = "appt-2",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = LocalDateTime.of(2026, 10, 25, 11, 0),
            isConfirmed = false,
            reason = "Endodoncia",
            status = AppointmentStatus.PENDING
        )
    )

    @Before
    fun setUp() {
        every { clinicClock.now() } returns LocalDateTime.of(2026, 10, 20, 8, 0)
        coEvery { getCurrentDoctorUseCase() } returns Result.success(testDoctor)
        coEvery { patientRepository.getPatients() } returns Result.success(testPatients)
        coEvery { getDoctorAppointmentsUseCase("doc-1") } returns Result.success(testAppointments)
    }

    @Test
    fun `when initialized, loads all doctor appointments`() = runTest {
        val viewModel = DoctorAppointmentsViewModel(
            getCurrentDoctorUseCase,
            getDoctorAppointmentsUseCase,
            toggleAppointmentConfirmationUseCase,
            patientRepository,
            clinicClock
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.appointments.size)
        assertEquals("doc-1", state.doctorId)
        assertEquals("Dr. Andrés Castro", state.doctorName)
    }

    @Test
    fun `when filtering appointments, returns correct subset`() = runTest {
        val viewModel = DoctorAppointmentsViewModel(
            getCurrentDoctorUseCase,
            getDoctorAppointmentsUseCase,
            toggleAppointmentConfirmationUseCase,
            patientRepository,
            clinicClock
        )
        advanceUntilIdle()

        // Today filter
        viewModel.onEvent(DoctorAppointmentsUiEvent.SelectFilter(DoctorAppointmentFilter.TODAY))
        val todayItems = viewModel.uiState.value.filteredAppointments(todayDate)
        assertEquals(1, todayItems.size)
        assertEquals("appt-1", todayItems.first().appointmentId)

        // Confirmed filter
        viewModel.onEvent(DoctorAppointmentsUiEvent.SelectFilter(DoctorAppointmentFilter.CONFIRMED))
        val confirmedItems = viewModel.uiState.value.filteredAppointments(todayDate)
        assertEquals(1, confirmedItems.size)
        assertTrue(confirmedItems.first().isConfirmed)

        // Unconfirmed filter
        viewModel.onEvent(DoctorAppointmentsUiEvent.SelectFilter(DoctorAppointmentFilter.UNCONFIRMED))
        val unconfirmedItems = viewModel.uiState.value.filteredAppointments(todayDate)
        assertEquals(1, unconfirmedItems.size)
        assertFalse(unconfirmedItems.first().isConfirmed)
    }

    @Test
    fun `when toggle confirm succeeds, toggles isConfirmed and emits effect`() = runTest {
        coEvery { toggleAppointmentConfirmationUseCase("appt-1", false) } returns Result.success(Unit)

        val viewModel = DoctorAppointmentsViewModel(
            getCurrentDoctorUseCase,
            getDoctorAppointmentsUseCase,
            toggleAppointmentConfirmationUseCase,
            patientRepository,
            clinicClock
        )

        viewModel.effects.test {
            advanceUntilIdle()

            viewModel.onEvent(DoctorAppointmentsUiEvent.ToggleConfirm("appt-1", true))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.appointments.first { it.appointmentId == "appt-1" }.isConfirmed)

            val effect = awaitItem()
            assertTrue(effect is DoctorAppointmentsUiEffect.ShowMessage)
        }
    }
}
