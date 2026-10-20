package com.example.biodontos.domain.usecase.billing

import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.repository.BillingRepository

class RecordPaymentUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(
        treatmentId: String,
        amountPesos: Long,
        paymentMethod: String = "EFECTIVO"
    ): Result<Payment> {
        val trimmedTreatmentId = treatmentId.trim()
        if (trimmedTreatmentId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador del tratamiento no puede estar vacío"))
        }
        if (amountPesos <= 0) {
            return Result.failure(IllegalArgumentException("El monto del abono debe ser mayor a cero pesos"))
        }

        val payment = Payment(
            treatmentId = trimmedTreatmentId,
            amount = CopAmount(amountPesos),
            paymentMethod = paymentMethod.trim().ifBlank { "EFECTIVO" }
        )

        return billingRepository.recordPayment(payment)
    }
}
