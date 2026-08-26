package com.example.biodontos.domain.usecase.odontogram

import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.repository.OdontogramRepository

class GetOdontogramUseCase(private val repository: OdontogramRepository) {
    suspend operator fun invoke(patientId: String): Result<List<Tooth>> {
        return repository.getOdontogram(patientId)
    }
}
