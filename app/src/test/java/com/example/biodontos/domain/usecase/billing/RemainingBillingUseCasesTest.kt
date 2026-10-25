package com.example.biodontos.domain.usecase.billing

import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.DoctorPaymentRecord
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.repository.BillingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RemainingBillingUseCasesTest {

    private val billingRepository: BillingRepository = mockk()

    @Test
    fun `CreateTreatmentUseCase validates inputs and creates treatment`() = runTest {
        val useCase = CreateTreatmentUseCase(billingRepository)

        assertTrue(useCase("", "Profilaxis", 80000L).isFailure)
        assertTrue(useCase("pat-1", "", 80000L).isFailure)
        assertTrue(useCase("pat-1", "Profilaxis", -100L).isFailure)

        val expectedTreatment = Treatment(
            id = "tr-1",
            patientId = PatientId("pat-1"),
            description = "Profilaxis",
            totalCost = CopAmount(80000L),
            status = "PENDIENTE"
        )
        coEvery { billingRepository.createTreatment(any()) } returns Result.success(expectedTreatment)

        val result = useCase("pat-1", "Profilaxis", 80000L)
        assertTrue(result.isSuccess)
        assertEquals(expectedTreatment, result.getOrNull())
    }

    @Test
    fun `GetDoctorRecentPaymentsUseCase returns payment records`() = runTest {
        val record = DoctorPaymentRecord(
            id = "pay-1",
            patientId = "pat-1",
            patientName = "Carlos Ruiz",
            treatmentId = "tr-1",
            treatmentDescription = "Ortodoncia",
            amount = CopAmount(120000L),
            paymentMethod = "TRANSFERENCIA",
            createdAt = LocalDateTime.of(2026, 10, 20, 15, 0)
        )
        coEvery { billingRepository.getRecentDoctorPayments(5) } returns Result.success(listOf(record))

        val useCase = GetDoctorRecentPaymentsUseCase(billingRepository)
        val result = useCase(limit = 5)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrDefault(emptyList()).size)
    }

    @Test
    fun `GetPatientTreatmentsUseCase validates patientId and fetches treatments`() = runTest {
        val useCase = GetPatientTreatmentsUseCase(billingRepository)

        assertTrue(useCase("   ").isFailure)

        val treatment = Treatment(
            id = "tr-2",
            patientId = PatientId("pat-1"),
            description = "Blanqueamiento",
            totalCost = CopAmount(250000L)
        )
        coEvery { billingRepository.getTreatmentsByPatientId("pat-1") } returns Result.success(listOf(treatment))

        val result = useCase("pat-1")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrDefault(emptyList()).size)
    }
}
