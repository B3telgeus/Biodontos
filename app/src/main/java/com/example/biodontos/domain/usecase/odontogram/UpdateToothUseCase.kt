package com.example.biodontos.domain.usecase.odontogram

import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface
import com.example.biodontos.domain.repository.OdontogramRepository

class UpdateToothUseCase(private val repository: OdontogramRepository) {
    suspend operator fun invoke(
        patientId: String,
        toothNumber: Int,
        surface: ToothSurface,
        condition: ToothCondition,
        comment: String?
    ): Result<Unit> {
        return repository.updateToothState(patientId, toothNumber, surface, condition, comment)
    }
}
