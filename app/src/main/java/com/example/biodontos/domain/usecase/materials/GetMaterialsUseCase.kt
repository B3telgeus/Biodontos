package com.example.biodontos.domain.usecase.materials

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.repository.MaterialRepository

class GetMaterialsUseCase(
    private val materialRepository: MaterialRepository
) {
    suspend operator fun invoke(
        includeInactive: Boolean = false,
        doctorId: DoctorId? = null
    ): Either<DomainError, List<ClinicalMaterial>> {
        return materialRepository.getMaterials(includeInactive = includeInactive, doctorId = doctorId).fold(
            onSuccess = { it.right() },
            onFailure = { err ->
                DomainError.NetworkOrPersistenceFailure(
                    err.message ?: "Error al obtener el catálogo de materiales clínicos."
                ).left()
            }
        )
    }

    suspend operator fun invoke(
        includeInactive: Boolean,
        doctorId: String?
    ): Either<DomainError, List<ClinicalMaterial>> = invoke(
        includeInactive = includeInactive,
        doctorId = doctorId?.ifBlank { null }?.let { DoctorId(it) }
    )
}
