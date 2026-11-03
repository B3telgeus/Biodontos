package com.example.biodontos.domain.usecase.materials

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.MaterialError
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.repository.MaterialRepository

class SetMaterialActiveUseCase(
    private val materialRepository: MaterialRepository
) {
    suspend operator fun invoke(
        materialId: MaterialId,
        isActive: Boolean
    ): Either<MaterialError, Unit> {
        return materialRepository.setMaterialActive(materialId, isActive).fold(
            onSuccess = { Unit.right() },
            onFailure = { err ->
                MaterialError.PersistenceError(
                    err.message ?: "Error al actualizar el estado del material clínico."
                ).left()
            }
        )
    }

    suspend operator fun invoke(
        materialId: String,
        isActive: Boolean
    ): Either<MaterialError, Unit> {
        val id = runCatching { MaterialId(materialId) }.getOrNull()
            ?: return MaterialError.PersistenceError("El identificador del material no es válido.").left()
        return invoke(id, isActive)
    }
}
