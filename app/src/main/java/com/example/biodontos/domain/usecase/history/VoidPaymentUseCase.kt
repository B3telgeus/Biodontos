package com.example.biodontos.domain.usecase.history

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.repository.BillingRepository

class VoidPaymentUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(
        payment: Payment,
        reason: String
    ): Either<DomainError, Unit> {
        if (payment.isVoided) {
            return ClinicalHistoryError.AlreadyVoided.left()
        }
        val cleanReason = reason.trim()
        if (cleanReason.length < MIN_REASON_LENGTH) {
            return ClinicalHistoryError.VoidReasonTooShort(MIN_REASON_LENGTH).left()
        }

        return billingRepository.voidPayment(payment.id, cleanReason).fold(
            onSuccess = { Unit.right() },
            onFailure = { err ->
                DomainError.NetworkOrPersistenceFailure(
                    err.message ?: "No se pudo anular el abono registrado."
                ).left()
            }
        )
    }

    companion object {
        const val MIN_REASON_LENGTH = 10
    }
}
