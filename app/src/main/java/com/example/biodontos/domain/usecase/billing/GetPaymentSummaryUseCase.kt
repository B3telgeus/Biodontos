package com.example.biodontos.domain.usecase.billing

import com.example.biodontos.domain.model.PatientPaymentSummary
import com.example.biodontos.domain.repository.BillingRepository

class GetPaymentSummaryUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(patientId: String): Result<PatientPaymentSummary> {
        if (patientId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador del paciente no puede estar vacío"))
        }
        return billingRepository.getPaymentSummaryByPatientId(patientId)
    }
}
