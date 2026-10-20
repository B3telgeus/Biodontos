package com.example.biodontos.domain.usecase.billing

import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.repository.BillingRepository

class GetPatientTreatmentsUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(patientId: String): Result<List<Treatment>> {
        val trimmed = patientId.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador del paciente no puede estar vacío"))
        }
        return billingRepository.getTreatmentsByPatientId(trimmed)
    }
}
