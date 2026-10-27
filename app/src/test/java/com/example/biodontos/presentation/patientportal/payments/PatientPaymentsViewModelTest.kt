package com.example.biodontos.presentation.patientportal.payments

import app.cash.turbine.test
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.PatientPaymentSummary
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.TreatmentBalance
import com.example.biodontos.domain.usecase.billing.GetPaymentSummaryUseCase
import com.example.biodontos.domain.usecase.patient.GetCurrentPatientUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PatientPaymentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentPatientUseCase: GetCurrentPatientUseCase = mockk()
    private val getPaymentSummaryUseCase: GetPaymentSummaryUseCase = mockk()

    private val testPatient = Patient(
        id = "patient-100",
        userId = "user-100",
        dniCedula = "12345678",
        firstName = "Camila",
        firstLastName = "Mora"
    )

    private val treatmentActive = Treatment(
        id = "tr-1",
        patientId = PatientId("patient-100"),
        description = "Ortodoncia brackets estéticos",
        totalCost = CopAmount(1_200_000),
        status = "ACTIVE"
    )

    private val treatmentCompleted = Treatment(
        id = "tr-2",
        patientId = PatientId("patient-100"),
        description = "Limpieza ultrasónica",
        totalCost = CopAmount(150_000),
        status = "COMPLETED"
    )

    private val payment1 = Payment(
        id = "pay-1",
        treatmentId = "tr-1",
        amount = CopAmount(400_000),
        paymentMethod = "Transferencia",
        createdAt = LocalDateTime.of(2026, 10, 5, 14, 30)
    )

    private val payment2 = Payment(
        id = "pay-2",
        treatmentId = "tr-2",
        amount = CopAmount(150_000),
        paymentMethod = "Efectivo",
        createdAt = LocalDateTime.of(2026, 10, 10, 9, 0)
    )

    private val balanceActive = TreatmentBalance(
        treatment = treatmentActive,
        paidAmount = CopAmount(400_000),
        payments = listOf(payment1)
    )

    private val balanceCompleted = TreatmentBalance(
        treatment = treatmentCompleted,
        paidAmount = CopAmount(150_000),
        payments = listOf(payment2)
    )

    private val testSummary = PatientPaymentSummary(
        totalPending = CopAmount(800_000),
        totalPaid = CopAmount(550_000),
        balances = listOf(balanceActive, balanceCompleted),
        recentPayments = listOf(payment2, payment1)
    )

    @Before
    fun setUp() {
        coEvery { getCurrentPatientUseCase() } returns Result.success(testPatient)
        coEvery { getPaymentSummaryUseCase("patient-100") } returns Result.success(testSummary)
    }

    @Test
    fun `when initialized successfully, uiState contains correct summary and balances`() = runTest {
        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertEquals("Camila Mora", state.patientName)
        assertEquals("patient-100", state.patientId)
        assertEquals(CopAmount(800_000), state.totalPending)
        assertEquals(CopAmount(550_000), state.totalPaid)
        assertEquals(2, state.balances.size)
        assertEquals(2, state.recentPayments.size)
        assertTrue(state.hasPendingBalance)
    }

    @Test
    fun `when patient session fails, uiState displays error`() = runTest {
        coEvery { getCurrentPatientUseCase() } returns Result.failure(Exception("Sesión no válida"))

        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No se pudo identificar la sesión del paciente", state.errorMessage)
    }

    @Test
    fun `when billing summary fails, uiState has errorMessage and effect is emitted`() = runTest {
        coEvery { getPaymentSummaryUseCase("patient-100") } returns Result.failure(RuntimeException("Error de conexión"))

        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)

        viewModel.effects.test {
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("Error de conexión", state.errorMessage)

            val effect = awaitItem()
            assertTrue(effect is PatientPaymentsUiEffect.ShowMessage)
        }
    }

    @Test
    fun `when filtering balances, filteredBalances returns appropriate subset`() = runTest {
        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        // Filter: ALL
        assertEquals(2, viewModel.uiState.value.filteredBalances.size)

        // Filter: PENDING
        viewModel.onEvent(PatientPaymentsUiEvent.SelectFilter(PaymentFilter.PENDING))
        assertEquals(1, viewModel.uiState.value.filteredBalances.size)
        assertEquals("tr-1", viewModel.uiState.value.filteredBalances.first().treatment.id)

        // Filter: COMPLETED
        viewModel.onEvent(PatientPaymentsUiEvent.SelectFilter(PaymentFilter.COMPLETED))
        assertEquals(1, viewModel.uiState.value.filteredBalances.size)
        assertEquals("tr-2", viewModel.uiState.value.filteredBalances.first().treatment.id)
    }

    @Test
    fun `when refresh event is triggered, reloads data successfully`() = runTest {
        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        viewModel.onEvent(PatientPaymentsUiEvent.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals(CopAmount(800_000), state.totalPending)
    }

    @Test
    fun `when retry event is triggered after error, re-executes loadPayments and recovers state`() = runTest {
        coEvery { getPaymentSummaryUseCase("patient-100") } returnsMany listOf(
            Result.failure(RuntimeException("Error temporal de red")),
            Result.success(testSummary)
        )

        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        assertEquals("Error temporal de red", viewModel.uiState.value.errorMessage)

        viewModel.onEvent(PatientPaymentsUiEvent.Retry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(CopAmount(800_000), state.totalPending)
        assertEquals(2, state.balances.size)
    }

    @Test
    fun `when selecting payment for receipt breakdown, uiState updates selectedPaymentForReceipt`() = runTest {
        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedPaymentForReceipt)

        viewModel.onEvent(PatientPaymentsUiEvent.ViewPaymentReceipt(payment1))

        assertEquals(payment1, viewModel.uiState.value.selectedPaymentForReceipt)
        assertEquals("pay-1", viewModel.uiState.value.selectedPaymentForReceipt?.id)
        assertEquals(CopAmount(400_000), viewModel.uiState.value.selectedPaymentForReceipt?.amount)
    }

    @Test
    fun `when dismissing receipt breakdown, uiState clears selectedPaymentForReceipt`() = runTest {
        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        viewModel.onEvent(PatientPaymentsUiEvent.ViewPaymentReceipt(payment2))
        assertEquals(payment2, viewModel.uiState.value.selectedPaymentForReceipt)

        viewModel.onEvent(PatientPaymentsUiEvent.DismissPaymentReceipt)
        assertNull(viewModel.uiState.value.selectedPaymentForReceipt)
    }

    @Test
    fun `when patient session has blank id, loadPayments sets error state without calling billing usecase`() = runTest {
        val blankPatient = testPatient.copy(id = "   ")
        coEvery { getCurrentPatientUseCase() } returns Result.success(blankPatient)

        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No se pudo identificar la sesión del paciente", state.errorMessage)
    }

    @Test
    fun `when balances have no recent payments and zero pending, uiState reflects zero amounts cleanly`() = runTest {
        val emptySummary = PatientPaymentSummary(
            totalPending = CopAmount(0),
            totalPaid = CopAmount(0),
            balances = emptyList(),
            recentPayments = emptyList()
        )
        coEvery { getPaymentSummaryUseCase("patient-100") } returns Result.success(emptySummary)

        val viewModel = PatientPaymentsViewModel(getCurrentPatientUseCase, getPaymentSummaryUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(CopAmount(0), state.totalPending)
        assertEquals(CopAmount(0), state.totalPaid)
        assertEquals(0, state.balances.size)
        assertEquals(0, state.recentPayments.size)
        assertEquals(0, state.filteredBalances.size)
        assertFalse(state.hasPendingBalance)
    }
}
