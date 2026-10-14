package com.example.biodontos.domain.usecase.billing

import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.repository.BillingRepository

class CreateTreatmentUseCase(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(
        patientId: String,
        description: String,
        totalCostPesos: Long
    ): Result<Treatment> {
        val trimmedPatientId = patientId.trim()
        if (trimmedPatientId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador del paciente no puede estar vacío"))
        }
        val trimmedDesc = description.trim()
        if (trimmedDesc.isBlank()) {
            return Result.failure(IllegalArgumentException("La descripción del tratamiento no puede estar vacía"))
        }
        if (totalCostPesos < 0) {
            return Result.failure(IllegalArgumentException("El costo total no puede ser negativo"))
        }

        val treatment = Treatment(
            patientId = PatientId(trimmedPatientId),
            description = trimmedDesc,
            totalCost = CopAmount(totalCostPesos),
            status = "PENDIENTE"
        )

        return billingRepository.createTreatment(treatment)
    }
}
