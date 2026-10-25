package com.example.biodontos.domain.usecase.history

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.repository.BillingRepository

class UpdateTreatmentStatusUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(
        treatment: Treatment,
        targetStatus: TreatmentStatus,
        reason: String? = null
    ): Either<DomainError, Unit> {
        val currentStatus = TreatmentStatus.fromString(treatment.status)
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return ClinicalHistoryError.InvalidStatusTransition(
                from = currentStatus.displayName,
                to = targetStatus.displayName
            ).left()
        }

        val cleanReason = reason?.trim()
        if (targetStatus == TreatmentStatus.CANCELADO && cleanReason.isNullOrBlank()) {
            return ClinicalHistoryError.StatusReasonRequired.left()
        }

        return billingRepository.updateTreatmentStatus(
            treatmentId = treatment.id,
            newStatus = targetStatus,
            reason = cleanReason
        ).fold(
            onSuccess = { Unit.right() },
            onFailure = { err ->
                DomainError.NetworkOrPersistenceFailure(
                    err.message ?: "No se pudo actualizar el estado del presupuesto."
                ).left()
            }
        )
    }
}
