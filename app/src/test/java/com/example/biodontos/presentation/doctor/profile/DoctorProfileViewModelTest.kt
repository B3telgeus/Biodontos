package com.example.biodontos.presentation.doctor.profile

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.usecase.appointment.CancelAppointmentUseCase
import com.example.biodontos.domain.usecase.appointment.ConfirmAppointmentUseCase
import com.example.biodontos.domain.usecase.appointment.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentSessionUseCase: GetCurrentSessionUseCase = mockk()
    private val doctorRepository: DoctorRepository = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()
    private val patientRepository: PatientRepository = mockk()
    private val logoutUseCase: LogoutUseCase = mockk(relaxed = true)

    private val dummyDoctorSession = UserSession(
        id = "doc-uid-1",
        email = "dr.carlos@biodontos.com",
        role = UserRole.DOCTOR,
        displayName = "Carlos Mendoza"
    )

    private val dummyDoctor = Doctor(
        id = DoctorId("doc-uid-1"),
        userId = "doc-uid-1",
        firstName = "Carlos",
        lastName = "Mendoza",
        specialty = "Ortodoncia & Estética",
        email = "dr.carlos@biodontos.com",
        licenseNumber = "OD-20381"
    )

    private val dummyPatient = Patient(
        id = "patient-1",
        userId = "patient-1",
        dniCedula = "10987654",
        firstName = "Andrés",
        firstLastName = "López",
        phone = "3109998877",
        email = "andres@test.com",
        consentSigned = true
    )

    private val pendingAppointment = Appointment(
        id = "apt-pending",
        patientId = "patient-1",
        doctorId = "doc-uid-1",
        startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
        status = AppointmentStatus.SCHEDULED,
        isConfirmed = false,
        reason = "Valoración General"
    )

    private val confirmedAppointment = Appointment(
        id = "apt-confirmed",
        patientId = "patient-1",
        doctorId = "doc-uid-1",
        startTime = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0),
        status = AppointmentStatus.SCHEDULED,
        isConfirmed = true,
        reason = "Control de brackets"
    )

    @Before
    fun setup() {
        coEvery { getCurrentSessionUseCase() } returns dummyDoctorSession
        coEvery { doctorRepository.getDoctorByUserId("doc-uid-1") } returns Result.success(dummyDoctor)
        coEvery { appointmentRepository.getAppointmentsByDoctorId("doc-uid-1") } returns Result.success(
            listOf(pendingAppointment, confirmedAppointment)
        )
        coEvery { patientRepository.getPatientById("patient-1") } returns Result.success(dummyPatient)
    }

    private val getDoctorAppointmentsUseCase by lazy { GetDoctorAppointmentsUseCase(appointmentRepository) }
    private val confirmAppointmentUseCase by lazy { ConfirmAppointmentUseCase(appointmentRepository) }
    private val cancelAppointmentUseCase by lazy { CancelAppointmentUseCase(appointmentRepository) }

    private fun createViewModel(): DoctorProfileViewModel {
        return DoctorProfileViewModel(
            getCurrentSessionUseCase = getCurrentSessionUseCase,
            doctorRepository = doctorRepository,
            getDoctorAppointmentsUseCase = getDoctorAppointmentsUseCase,
            confirmAppointmentUseCase = confirmAppointmentUseCase,
            cancelAppointmentUseCase = cancelAppointmentUseCase,
            patientRepository = patientRepository,
            logoutUseCase = logoutUseCase
        )
    }

    @Test
    fun `initialization loads doctor profile, appointments, and associates patient details`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Carlos", state.doctor?.firstName)
        assertEquals("Ortodoncia & Estética", state.doctor?.specialty)
        assertEquals(2, state.appointments.size)

        // Verificación de citas pendientes de confirmación
        assertEquals(1, state.pendingAppointments.size)
        assertEquals("apt-pending", state.pendingAppointments.first().id)
        assertFalse(state.pendingAppointments.first().isConfirmed)

        // Verificación de citas confirmadas
        assertEquals(1, state.confirmedAppointments.size)
        assertEquals("apt-confirmed", state.confirmedAppointments.first().id)
        assertTrue(state.confirmedAppointments.first().isConfirmed)

        // Verificación de asociación de paciente
        assertTrue(state.patientsMap.containsKey("patient-1"))
        assertEquals("Andrés López", state.patientsMap["patient-1"]?.fullName)
    }

    @Test
    fun `confirmAppointment updates confirmation status to true`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { appointmentRepository.updateConfirmationStatus("apt-pending", true) } returns Result.success(Unit)

        viewModel.onEvent(DoctorProfileEvent.ConfirmAppointment("apt-pending"))
        advanceUntilIdle()

        coVerify { appointmentRepository.updateConfirmationStatus("apt-pending", isConfirmed = true) }
        coVerify(atLeast = 2) { appointmentRepository.getAppointmentsByDoctorId("doc-uid-1") }
    }

    @Test
    fun `cancelAppointment updates status to cancelled with reason`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { appointmentRepository.cancelAppointment("apt-pending", "No hay disponibilidad de instrumental") } returns Result.success(Unit)

        viewModel.onEvent(DoctorProfileEvent.CancelAppointment("apt-pending", "No hay disponibilidad de instrumental"))
        advanceUntilIdle()

        coVerify { appointmentRepository.cancelAppointment("apt-pending", "No hay disponibilidad de instrumental") }
    }

    @Test
    fun `logout triggers LogoutUseCase and emits NavigateToLogin effect`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DoctorProfileEvent.Logout)
        advanceUntilIdle()

        coVerify { logoutUseCase() }
        val effect = viewModel.effects.first()
        assertEquals(DoctorProfileEffect.NavigateToLogin, effect)
    }
}
