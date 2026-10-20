package com.example.biodontos.domain.usecase.billing

import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.PatientPaymentSummary
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.TreatmentBalance
import com.example.biodontos.domain.repository.BillingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class GetPaymentSummaryUseCaseTest {

    private val billingRepository: BillingRepository = mockk()
    private lateinit var useCase: GetPaymentSummaryUseCase

    private val testSummary = PatientPaymentSummary(
        totalPending = CopAmount(300_000),
        totalPaid = CopAmount(200_000),
        balances = listOf(
            TreatmentBalance(
                treatment = Treatment(
                    id = "tr-1",
                    patientId = PatientId("pat-123"),
                    description = "Ortodoncia brackets",
                    totalCost = CopAmount(500_000),
                    status = "ACTIVE"
                ),
                paidAmount = CopAmount(200_000),
                payments = listOf(
                    Payment(
                        id = "pay-1",
                        treatmentId = "tr-1",
                        amount = CopAmount(200_000),
                        paymentMethod = "Transferencia Bancaria",
                        createdAt = LocalDateTime.of(2026, 10, 1, 10, 0)
                    )
                )
            )
        ),
        recentPayments = listOf(
            Payment(
                id = "pay-1",
                treatmentId = "tr-1",
                amount = CopAmount(200_000),
                paymentMethod = "Transferencia Bancaria",
                createdAt = LocalDateTime.of(2026, 10, 1, 10, 0)
            )
        )
    )

    @Before
    fun setUp() {
        useCase = GetPaymentSummaryUseCase(billingRepository)
    }

    @Test
    fun `when patientId is blank, returns failure without calling repository`() = runTest {
        val result = useCase("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `when repository returns success, returns summary`() = runTest {
        coEvery { billingRepository.getPaymentSummaryByPatientId("pat-123") } returns Result.success(testSummary)

        val result = useCase("pat-123")

        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertEquals(CopAmount(300_000), summary?.totalPending)
        assertEquals(CopAmount(200_000), summary?.totalPaid)
        assertEquals(1, summary?.balances?.size)
        assertEquals(1, summary?.recentPayments?.size)
    }

    @Test
    fun `when repository returns failure, propagates error`() = runTest {
        val error = RuntimeException("Error al conectar con la base de datos")
        coEvery { billingRepository.getPaymentSummaryByPatientId("pat-123") } returns Result.failure(error)

        val result = useCase("pat-123")

        assertTrue(result.isFailure)
        assertEquals("Error al conectar con la base de datos", result.exceptionOrNull()?.message)
    }
}
