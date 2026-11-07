package com.example.biodontos.presentation.patientportal.home

import app.cash.turbine.test
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.PatientPaymentSummary
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.AuthRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.repository.PatientRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PatientHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk()
    private val patientRepository: PatientRepository = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()
    private val billingRepository: BillingRepository = mockk()
    private val doctorRepository: DoctorRepository = mockk()

    private val testSession = UserSession(
        id = "user-pat-1",
        email = "paciente@test.com",
        role = UserRole.PATIENT,
        displayName = "Pepito"
    )

    private val testPatient = Patient(
        id = "patient-1",
        userId = "user-pat-1",
        dniCedula = "12345678",
        firstName = "Pepito",
        firstLastName = "Pérez"
    )

    @Before
    fun setUp() {
        coEvery { authRepository.getCurrentSession() } returns testSession
        coEvery { authRepository.logout() } returns Result.success(Unit)
        coEvery { patientRepository.getPatientByUserId(any()) } returns Result.success(testPatient)
        coEvery { appointmentRepository.getAppointmentsByPatientId(any()) } returns Result.success(emptyList())
        coEvery { billingRepository.getPaymentSummaryByPatientId(any()) } returns Result.success(
            PatientPaymentSummary(
                totalPending = CopAmount(0),
                totalPaid = CopAmount(0),
                balances = emptyList(),
                recentPayments = emptyList()
            )
        )
        coEvery { doctorRepository.getDoctors() } returns Result.success(emptyList())
    }

    private fun createViewModel(): PatientHomeViewModel {
        return PatientHomeViewModel(
            authRepository = authRepository,
            patientRepository = patientRepository,
            appointmentRepository = appointmentRepository,
            billingRepository = billingRepository,
            doctorRepository = doctorRepository
        )
    }

    @Test
    fun `loadDashboard successfully populates uiState when user is authenticated patient`() = runTest {
        coEvery { authRepository.getCurrentSession() } returns testSession
        coEvery { patientRepository.getPatientByUserId("user-pat-1") } returns Result.success(testPatient)

        val upcomingAppointment = Appointment(
            id = "appt-1",
            patientId = "patient-1",
            doctorId = "doc-1",
            startTime = LocalDateTime.now().plusDays(2),
            status = AppointmentStatus.CONFIRMED,
            reason = "Control de ortodoncia"
        )
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(listOf(upcomingAppointment))

        val billingSummary = PatientPaymentSummary(
            totalPending = CopAmount(500000),
            totalPaid = CopAmount(100000),
            balances = emptyList(),
            recentPayments = emptyList()
        )
        coEvery { billingRepository.getPaymentSummaryByPatientId("patient-1") } returns Result.success(billingSummary)

        val testDoctor = Doctor(
            id = DoctorId("doc-1"),
            firstName = "Pepita",
            lastName = "Pérez",
            specialty = "Ortodoncia",
            licenseNumber = "1234",
            email = "doc@test.com"
        )
        coEvery { doctorRepository.getDoctors() } returns Result.success(listOf(testDoctor))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Pepito", state.patientName)
        assertEquals("patient-1", state.patientId)
        assertNotNull(state.nextAppointment)
        assertEquals("Control de ortodoncia", state.nextAppointment?.reason)
        assertEquals(500000L, state.pendingBalance.pesos)
        assertEquals(1, state.doctors.size)
        assertEquals("doc-1", state.doctors.first().id.value)
    }

    @Test
    fun `loadDashboard exposes empty doctors list when patient has no appointments with doctors`() = runTest {
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(emptyList())

        val testDoctor = Doctor(
            id = DoctorId("doc-1"),
            firstName = "Pepita",
            lastName = "Pérez",
            specialty = "Ortodoncia"
        )
        coEvery { doctorRepository.getDoctors() } returns Result.success(listOf(testDoctor))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.doctors.size)
    }

    @Test
    fun `loadDashboard exposes only the most recent booked doctor when patient has multiple appointments`() = runTest {
        val olderAppointment = Appointment(
            id = "appt-old",
            patientId = "patient-1",
            doctorId = "doc-1",
            startTime = LocalDateTime.now().minusDays(10),
            status = AppointmentStatus.COMPLETED
        )
        val newerAppointment = Appointment(
            id = "appt-new",
            patientId = "patient-1",
            doctorId = "doc-2",
            startTime = LocalDateTime.now().plusDays(3),
            status = AppointmentStatus.CONFIRMED
        )
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(
            listOf(olderAppointment, newerAppointment)
        )

        val doc1 = Doctor(id = DoctorId("doc-1"), firstName = "Carlos", lastName = "Pérez", specialty = "General")
        val doc2 = Doctor(id = DoctorId("doc-2"), firstName = "Ana", lastName = "Gómez", specialty = "Endodoncia")
        coEvery { doctorRepository.getDoctors() } returns Result.success(listOf(doc1, doc2))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.doctors.size)
        assertEquals("doc-2", state.doctors.first().id.value)
        assertEquals("Ana", state.doctors.first().firstName)
    }

    @Test
    fun `onEvent BookWithDoctor emits NavigateToBooking effect`() = runTest {
        coEvery { authRepository.getCurrentSession() } returns testSession
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PatientHomeUiEvent.BookWithDoctor("doc-99"))
            val effect = awaitItem()
            assertEquals(PatientHomeUiEffect.NavigateToBooking("doc-99"), effect)
        }
    }

    @Test
    fun `onEvent Logout calls authRepository and emits NavigateToLogin`() = runTest {
        coEvery { authRepository.getCurrentSession() } returns testSession
        coEvery { authRepository.logout() } returns Result.success(Unit)

        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PatientHomeUiEvent.Logout)
            val effect = awaitItem()
            assertEquals(PatientHomeUiEffect.NavigateToLogin, effect)
        }

        coVerify { authRepository.logout() }
    }

    @Test
    fun `loadDashboard emits NavigateToLogin when session is null`() = runTest {
        coEvery { authRepository.getCurrentSession() } returns null

        val viewModel = createViewModel()

        viewModel.effects.test {
            val effect = awaitItem()
            assertEquals(PatientHomeUiEffect.NavigateToLogin, effect)
        }
    }

    @Test
    fun `loadDashboard emits NavigateToLogin when session role is not PATIENT`() = runTest {
        coEvery { authRepository.getCurrentSession() } returns testSession.copy(role = UserRole.DOCTOR)

        val viewModel = createViewModel()

        viewModel.effects.test {
            val effect = awaitItem()
            assertEquals(PatientHomeUiEffect.NavigateToLogin, effect)
        }
    }

    @Test
    fun `loadDashboard populates errorMessage when appointments and billing fail`() = runTest {
        coEvery { appointmentRepository.getAppointmentsByPatientId(any()) } returns Result.failure(RuntimeException("Network error"))
        coEvery { billingRepository.getPaymentSummaryByPatientId(any()) } returns Result.failure(RuntimeException("Network error"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No fue posible actualizar tu información clínica. Comprueba tu conexión.", state.errorMessage)
    }

    @Test
    fun `onEvent Refresh triggers reload and clears previous errorMessage`() = runTest {
        // Primero forzar error
        coEvery { appointmentRepository.getAppointmentsByPatientId(any()) } returns Result.failure(RuntimeException("Network error"))
        coEvery { billingRepository.getPaymentSummaryByPatientId(any()) } returns Result.failure(RuntimeException("Network error"))

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("No fue posible actualizar tu información clínica. Comprueba tu conexión.", viewModel.uiState.value.errorMessage)

        // Ahora reintentar con éxito
        coEvery { appointmentRepository.getAppointmentsByPatientId(any()) } returns Result.success(emptyList())
        coEvery { billingRepository.getPaymentSummaryByPatientId(any()) } returns Result.success(
            PatientPaymentSummary(
                totalPending = CopAmount(0),
                totalPaid = CopAmount(0),
                balances = emptyList(),
                recentPayments = emptyList()
            )
        )

        viewModel.onEvent(PatientHomeUiEvent.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `onEvent NavigateToAppointments emits NavigateToAppointments effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PatientHomeUiEvent.NavigateToAppointments)
            val effect = awaitItem()
            assertEquals(PatientHomeUiEffect.NavigateToAppointments, effect)
        }
    }

    @Test
    fun `onEvent NavigateToPayments emits NavigateToPayments effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PatientHomeUiEvent.NavigateToPayments)
            val effect = awaitItem()
            assertEquals(PatientHomeUiEffect.NavigateToPayments, effect)
        }
    }

    @Test
    fun `onEvent NavigateToDoctors emits NavigateToDoctors effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(PatientHomeUiEvent.NavigateToDoctors)
            val effect = awaitItem()
            assertEquals(PatientHomeUiEffect.NavigateToDoctors, effect)
        }
    }

    @Test
    fun `loadDashboard uses fallback when patientRepository returns failure`() = runTest {
        coEvery { patientRepository.getPatientByUserId("user-pat-1") } returns Result.failure(RuntimeException("Patient not found"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("user-pat-1", state.patientId)
        assertEquals("Pepito", state.patientName)
    }

    @Test
    fun `loadDashboard recovers doctor from appointment doctorName when not present in doctorsList`() = runTest {
        val appointment = Appointment(
            id = "appt-ext",
            patientId = "patient-1",
            doctorId = "doc-external",
            doctorName = "Dr. Fernando Ruiz",
            startTime = LocalDateTime.now().minusDays(1),
            status = AppointmentStatus.COMPLETED
        )
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(listOf(appointment))
        coEvery { doctorRepository.getDoctors() } returns Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.doctors.size)
        assertEquals("doc-external", state.doctors.first().id.value)
        assertEquals("Fernando", state.doctors.first().firstName)
        assertEquals("Ruiz", state.doctors.first().lastName)
    }

    @Test
    fun `loadDashboard recovers fallback doctor when appointment doctorName is null and doctor not present in doctorsList`() = runTest {
        val appointment = Appointment(
            id = "appt-anon",
            patientId = "patient-1",
            doctorId = "doc-anon",
            doctorName = null,
            startTime = LocalDateTime.now().minusDays(2),
            status = AppointmentStatus.COMPLETED
        )
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(listOf(appointment))
        coEvery { doctorRepository.getDoctors() } returns Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.doctors.size)
        assertEquals("doc-anon", state.doctors.first().id.value)
        assertEquals("Doctor", state.doctors.first().firstName)
        assertEquals("Especialista", state.doctors.first().lastName)
    }

    @Test
    fun `loadDashboard cleans honorific titles like Dr dot Dra and Doctor without leaving empty names`() = runTest {
        val appointment = Appointment(
            id = "appt-title-only",
            patientId = "patient-1",
            doctorId = "doc-title",
            doctorName = "Dr.",
            startTime = LocalDateTime.now().minusDays(1),
            status = AppointmentStatus.COMPLETED
        )
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(listOf(appointment))
        coEvery { doctorRepository.getDoctors() } returns Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.doctors.size)
        val doctor = state.doctors.first()
        assertEquals("Doctor", doctor.firstName)
        assertEquals("Especialista", doctor.lastName)
        assertEquals("Dr. Doctor Especialista", doctor.displayName)
    }

    @Test
    fun `loadDashboard trims doctorId and correctly matches doctor in directory`() = runTest {
        val appointment = Appointment(
            id = "appt-spaced",
            patientId = "patient-1",
            doctorId = "  doc-padded  ",
            startTime = LocalDateTime.now().minusDays(1),
            status = AppointmentStatus.CONFIRMED
        )
        val dirDoctor = Doctor(
            id = DoctorId("doc-padded"),
            firstName = "Valentina",
            lastName = "Morales",
            specialty = "Periodoncia",
            clinicAddress = "Sede Norte Cons 301"
        )
        coEvery { appointmentRepository.getAppointmentsByPatientId("patient-1") } returns Result.success(listOf(appointment))
        coEvery { doctorRepository.getDoctors() } returns Result.success(listOf(dirDoctor))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.doctors.size)
        val doctor = state.doctors.first()
        assertEquals("Valentina", doctor.firstName)
        assertEquals("Morales", doctor.lastName)
        assertEquals("Sede Norte Cons 301", doctor.clinicAddress)
    }
}

