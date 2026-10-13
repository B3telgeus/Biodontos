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
}
