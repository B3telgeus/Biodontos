package com.example.biodontos.presentation.doctor.home

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
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentDoctorUseCase: GetCurrentDoctorUseCase = mockk()
    private val getDoctorAppointmentsUseCase: GetDoctorAppointmentsUseCase = mockk()
    private val toggleAppointmentConfirmationUseCase: ToggleAppointmentConfirmationUseCase = mockk()
    private val patientRepository: PatientRepository = mockk()
    private val logoutUseCase: LogoutUseCase = mockk()
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
        Patient(id = "pat-1", dniCedula = "111", firstName = "Dora", firstLastName = "Hernández")
    )

    private val testAppointments = listOf(
        Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = LocalDateTime.of(2026, 10, 20, 8, 0),
            isConfirmed = false,
            reason = "Profilaxis",
            status = AppointmentStatus.PENDING
        )
    )

    @Before
    fun setUp() {
        every { clinicClock.now() } returns LocalDateTime.of(2026, 10, 20, 7, 0)
        coEvery { getCurrentDoctorUseCase() } returns Result.success(testDoctor)
        coEvery { patientRepository.getPatients() } returns Result.success(testPatients)
        coEvery { getDoctorAppointmentsUseCase("doc-1") } returns Result.success(testAppointments)
        coEvery { logoutUseCase() } returns Result.success(Unit)
    }

    @Test
    fun `when initialized successfully, uiState contains doctor and mapped appointments`() = runTest {
        val viewModel = DoctorHomeViewModel(
            getCurrentDoctorUseCase,
            getDoctorAppointmentsUseCase,
            toggleAppointmentConfirmationUseCase,
            patientRepository,
            logoutUseCase,
            clinicClock
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Dr. Andrés Castro", state.doctorName)
        assertEquals(1, state.patientsCount)
        assertEquals(1, state.todayAppointmentsCount)
        assertEquals(1, state.upcomingAppointments.size)
        assertEquals("Dora Hernández", state.upcomingAppointments.first().patientName)
        assertFalse(state.upcomingAppointments.first().isConfirmed)
    }

    @Test
    fun `when toggle confirm succeeds, updates confirmation status and emits message`() = runTest {
        coEvery { toggleAppointmentConfirmationUseCase("appt-1", true) } returns Result.success(Unit)

        val viewModel = DoctorHomeViewModel(
            getCurrentDoctorUseCase,
            getDoctorAppointmentsUseCase,
            toggleAppointmentConfirmationUseCase,
            patientRepository,
            logoutUseCase,
            clinicClock
        )

        viewModel.effects.test {
            advanceUntilIdle()

            viewModel.onEvent(DoctorHomeUiEvent.ToggleConfirmAppointment("appt-1", false))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.upcomingAppointments.first().isConfirmed)

            val effect = awaitItem()
            assertTrue(effect is DoctorHomeUiEffect.ShowMessage)
            assertEquals("Cita confirmada con éxito", (effect as DoctorHomeUiEffect.ShowMessage).message)
        }
    }

    @Test
    fun `when logout event is triggered, calls logout and navigates to login`() = runTest {
        val viewModel = DoctorHomeViewModel(
            getCurrentDoctorUseCase,
            getDoctorAppointmentsUseCase,
            toggleAppointmentConfirmationUseCase,
            patientRepository,
            logoutUseCase,
            clinicClock
        )

        viewModel.effects.test {
            advanceUntilIdle()

            viewModel.onEvent(DoctorHomeUiEvent.Logout)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is DoctorHomeUiEffect.NavigateToLogin)
        }
    }
}
