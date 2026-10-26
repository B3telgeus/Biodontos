package com.example.biodontos.presentation.doctor.home

import app.cash.turbine.test
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.DoctorPaymentRecord
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.repository.PatientRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.appointments.GetDoctorAppointmentsUseCase
import com.example.biodontos.domain.usecase.appointments.ToggleAppointmentConfirmationUseCase
import com.example.biodontos.domain.usecase.auth.LogoutUseCase
import com.example.biodontos.domain.usecase.billing.CreateTreatmentUseCase
import com.example.biodontos.domain.usecase.billing.GetDoctorRecentPaymentsUseCase
import com.example.biodontos.domain.usecase.billing.GetPatientTreatmentsUseCase
import com.example.biodontos.domain.usecase.billing.RecordPaymentUseCase
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val getDoctorRecentPaymentsUseCase: GetDoctorRecentPaymentsUseCase = mockk()
    private val recordPaymentUseCase: RecordPaymentUseCase = mockk()
    private val createTreatmentUseCase: CreateTreatmentUseCase = mockk()
    private val getPatientTreatmentsUseCase: GetPatientTreatmentsUseCase = mockk()

    private val testDoctor = Doctor(
        id = DoctorId("doc-1"),
        firstName = "Andrés",
        lastName = "Castro",
        specialty = "Ortodoncia",
        licenseNumber = "12345",
        email = "andres@test.com",
        photoUrl = "https://example.com/doctor.jpg"
    )

    private val testPatients = listOf(
        Patient(id = "pat-1", dniCedula = "111", firstName = "Dora", firstLastName = "Hernández", consentSigned = true),
        Patient(id = "pat-2", dniCedula = "222", firstName = "Luis", firstLastName = "Martínez", consentSigned = false)
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

    private val testPaymentRecords = listOf(
        DoctorPaymentRecord(
            id = "pay-100",
            patientId = "pat-1",
            patientName = "Dora Hernández",
            treatmentId = "tr-100",
            treatmentDescription = "Ortodoncia Brackets",
            amount = CopAmount(350_000L),
            paymentMethod = "EFECTIVO",
            createdAt = LocalDateTime.of(2026, 10, 20, 10, 0)
        )
    )

    private fun createViewModel() = DoctorHomeViewModel(
        getCurrentDoctorUseCase = getCurrentDoctorUseCase,
        getDoctorAppointmentsUseCase = getDoctorAppointmentsUseCase,
        toggleAppointmentConfirmationUseCase = toggleAppointmentConfirmationUseCase,
        patientRepository = patientRepository,
        logoutUseCase = logoutUseCase,
        clinicClock = clinicClock,
        getDoctorRecentPaymentsUseCase = getDoctorRecentPaymentsUseCase,
        recordPaymentUseCase = recordPaymentUseCase,
        createTreatmentUseCase = createTreatmentUseCase,
        getPatientTreatmentsUseCase = getPatientTreatmentsUseCase
    )

    @Before
    fun setUp() {
        every { clinicClock.now() } returns LocalDateTime.of(2026, 10, 20, 7, 0)
        coEvery { getCurrentDoctorUseCase() } returns Result.success(testDoctor)
        coEvery { patientRepository.getPatients() } returns Result.success(testPatients)
        coEvery { getDoctorAppointmentsUseCase("doc-1") } returns Result.success(testAppointments)
        coEvery { getDoctorRecentPaymentsUseCase(any()) } returns Result.success(testPaymentRecords)
        coEvery { logoutUseCase() } returns Result.success(Unit)
    }

    @Test
    fun `when initialized successfully, uiState contains doctor, appointments and real payments`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Dr. Andrés Castro", state.doctorName)
        assertEquals("https://example.com/doctor.jpg", state.doctorPhotoUrl)
        assertEquals(2, state.patientsCount)
        assertEquals(1, state.consentedPatientsCount)
        assertEquals(1, state.pendingConsentCount)
        assertEquals(1, state.todayAppointmentsCount)
        assertEquals(1, state.upcomingAppointments.size)
        assertEquals("pat-1", state.upcomingAppointments.first().patientId)
        assertEquals("Dora Hernández", state.upcomingAppointments.first().patientName)
        assertFalse(state.upcomingAppointments.first().isConfirmed)

        // Verificación de pagos reales cargados
        assertEquals(1, state.recentPayments.size)
        assertEquals("pat-1", state.recentPayments.first().patientId)
        assertEquals("Dora Hernández", state.recentPayments.first().patientName)
        assertEquals("Ortodoncia Brackets", state.recentPayments.first().treatmentDescription)
        assertEquals(350_000L, state.recentPayments.first().amountCop.pesos)
        assertEquals(350_000L, state.recentTotalCollectedCop.pesos)
    }

    @Test
    fun `when upcoming appointments are filtered, excludes cancelled, completed, and past appointments`() = runTest {
        val mixedAppointments = listOf(
            // Válida: hoy en el futuro (8:00 vs reloj a las 7:00)
            Appointment(
                id = "appt-valid-1",
                patientId = "pat-1",
                doctorId = "doc-1",
                startTime = LocalDateTime.of(2026, 10, 20, 8, 0),
                isConfirmed = false,
                status = AppointmentStatus.PENDING
            ),
            // Cancelada: debe ser excluida
            Appointment(
                id = "appt-cancelled",
                patientId = "pat-1",
                doctorId = "doc-1",
                startTime = LocalDateTime.of(2026, 10, 20, 9, 0),
                isConfirmed = false,
                status = AppointmentStatus.CANCELLED
            ),
            // Completada: debe ser excluida
            Appointment(
                id = "appt-completed",
                patientId = "pat-1",
                doctorId = "doc-1",
                startTime = LocalDateTime.of(2026, 10, 20, 10, 0),
                isConfirmed = true,
                status = AppointmentStatus.COMPLETED
            ),
            // Pasada (ayer 19 de Octubre): debe ser excluida
            Appointment(
                id = "appt-past",
                patientId = "pat-1",
                doctorId = "doc-1",
                startTime = LocalDateTime.of(2026, 10, 19, 14, 0),
                isConfirmed = true,
                status = AppointmentStatus.CONFIRMED
            )
        )
        coEvery { getDoctorAppointmentsUseCase("doc-1") } returns Result.success(mixedAppointments)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.upcomingAppointments.size)
        assertEquals("appt-valid-1", state.upcomingAppointments.first().appointmentId)
    }

    @Test
    fun `when clearSelectedPatient is dispatched, resets selectedPatient and treatments`() = runTest {
        val testPatient = testPatients.first()
        val treatments = listOf(
            Treatment(id = "tr-1", patientId = PatientId("pat-1"), description = "Profilaxis", totalCost = CopAmount(100_000L))
        )
        coEvery { getPatientTreatmentsUseCase("pat-1") } returns Result.success(treatments)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DoctorHomeUiEvent.SelectPatientForPayment(testPatient))
        advanceUntilIdle()

        assertEquals(testPatient, viewModel.uiState.value.selectedPatient)
        assertEquals(1, viewModel.uiState.value.availableTreatments.size)

        viewModel.onEvent(DoctorHomeUiEvent.ClearSelectedPatient)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedPatient)
        assertNull(viewModel.uiState.value.selectedTreatment)
        assertTrue(viewModel.uiState.value.availableTreatments.isEmpty())
        assertFalse(viewModel.uiState.value.isLoadingTreatments)
    }

    @Test
    fun `when doctor session fails, sets errorMessage`() = runTest {
        coEvery { getCurrentDoctorUseCase() } returns Result.failure(Exception("Sin sesión activa"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals("No se pudo identificar la sesión del profesional", state.errorMessage)
    }

    @Test
    fun `when parallel load throws exception, catches and sets errorMessage`() = runTest {
        coEvery { patientRepository.getPatients() } throws RuntimeException("Error fatal de base de datos")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals("Error fatal de base de datos", state.errorMessage)
    }

    @Test
    fun `when toggle confirm succeeds, updates confirmation status and emits message`() = runTest {
        coEvery { toggleAppointmentConfirmationUseCase("appt-1", true) } returns Result.success(Unit)

        val viewModel = createViewModel()

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
    fun `when submit payment is triggered with existing treatment, records payment successfully`() = runTest {
        val recordedPayment = Payment(
            id = "pay-200",
            treatmentId = "tr-100",
            amount = CopAmount(200_000L),
            paymentMethod = "TRANSFERENCIA"
        )
        coEvery { recordPaymentUseCase("tr-100", 200_000L, "TRANSFERENCIA") } returns Result.success(recordedPayment)

        val viewModel = createViewModel()

        viewModel.effects.test {
            advanceUntilIdle()

            viewModel.onEvent(
                DoctorHomeUiEvent.SubmitPayment(
                    patientId = "pat-1",
                    treatmentId = "tr-100",
                    newTreatmentDescription = null,
                    newTreatmentCost = null,
                    amount = 200_000L,
                    paymentMethod = "TRANSFERENCIA"
                )
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { recordPaymentUseCase("tr-100", 200_000L, "TRANSFERENCIA") }

            val effect = awaitItem()
            assertTrue(effect is DoctorHomeUiEffect.ShowMessage)
            assertEquals("¡Abono registrado con éxito!", (effect as DoctorHomeUiEffect.ShowMessage).message)
        }
    }

    @Test
    fun `when submit payment has no treatmentId, creates treatment first and then records payment`() = runTest {
        val createdTreatment = Treatment(
            id = "tr-new-99",
            patientId = PatientId("pat-1"),
            description = "Diseño de sonrisa",
            totalCost = CopAmount(1_200_000L)
        )
        val recordedPayment = Payment(
            id = "pay-new-99",
            treatmentId = "tr-new-99",
            amount = CopAmount(400_000L),
            paymentMethod = "EFECTIVO"
        )
        coEvery { createTreatmentUseCase("pat-1", "Diseño de sonrisa", 1_200_000L) } returns Result.success(createdTreatment)
        coEvery { recordPaymentUseCase("tr-new-99", 400_000L, "EFECTIVO") } returns Result.success(recordedPayment)

        val viewModel = createViewModel()

        viewModel.effects.test {
            advanceUntilIdle()

            viewModel.onEvent(
                DoctorHomeUiEvent.SubmitPayment(
                    patientId = "pat-1",
                    treatmentId = null,
                    newTreatmentDescription = "Diseño de sonrisa",
                    newTreatmentCost = 1_200_000L,
                    amount = 400_000L,
                    paymentMethod = "EFECTIVO"
                )
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { createTreatmentUseCase("pat-1", "Diseño de sonrisa", 1_200_000L) }
            coVerify(exactly = 1) { recordPaymentUseCase("tr-new-99", 400_000L, "EFECTIVO") }

            val effect = awaitItem()
            assertTrue(effect is DoctorHomeUiEffect.ShowMessage)
            assertEquals("¡Abono registrado con éxito!", (effect as DoctorHomeUiEffect.ShowMessage).message)
        }
    }

    @Test
    fun `when logout event is triggered, calls logout and navigates to login`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            advanceUntilIdle()

            viewModel.onEvent(DoctorHomeUiEvent.Logout)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is DoctorHomeUiEffect.NavigateToLogin)
        }
    }
}
