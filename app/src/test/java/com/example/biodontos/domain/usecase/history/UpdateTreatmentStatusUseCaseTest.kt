package com.example.biodontos.domain.usecase.history

import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.repository.BillingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTreatmentStatusUseCaseTest {

    private val billingRepository: BillingRepository = mockk()
    private val useCase = UpdateTreatmentStatusUseCase(billingRepository)

    private val pendingTreatment = Treatment(
        id = "tr-1",
        patientId = PatientId("pat-1"),
        description = "Ortodoncia correctiva",
        totalCost = CopAmount(2500000L),
        status = "PENDIENTE"
    )

    @Test
    fun `rejects invalid status transition such as PENDIENTE directly to COMPLETADO`() = runTest {
        val result = useCase(pendingTreatment, TreatmentStatus.COMPLETADO)
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ClinicalHistoryError.InvalidStatusTransition)
    }

    @Test
    fun `requires reason when transitioning to CANCELADO`() = runTest {
        val result = useCase(pendingTreatment, TreatmentStatus.CANCELADO, reason = "  ")
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ClinicalHistoryError.StatusReasonRequired)
    }

    @Test
    fun `allows valid transition to APROBADO and persists status`() = runTest {
        coEvery {
            billingRepository.updateTreatmentStatus("tr-1", TreatmentStatus.APROBADO, null)
        } returns Result.success(Unit)

        val result = useCase(pendingTreatment, TreatmentStatus.APROBADO)
        assertTrue(result.isRight())
        coVerify(exactly = 1) {
            billingRepository.updateTreatmentStatus("tr-1", TreatmentStatus.APROBADO, null)
        }
    }
}
