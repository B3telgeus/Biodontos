package com.example.biodontos.domain.usecase.materials

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.MaterialError
import com.example.biodontos.domain.repository.MaterialRepository
import com.example.biodontos.domain.scheduling.ClinicClock

class SaveMaterialUseCase(
    private val materialRepository: MaterialRepository,
    private val clinicClock: ClinicClock
) {
    suspend operator fun invoke(material: ClinicalMaterial): Either<MaterialError, ClinicalMaterial> {
        val cleanName = material.name.trim()
        if (cleanName.isBlank()) {
            return MaterialError.BlankName.left()
        }

        val cleanInvima = material.invimaRegistration.trim()
        if (cleanInvima.isBlank()) {
            return MaterialError.BlankInvimaRegistration.left()
        }

        val cleanLot = material.batchLotNumber.trim()
        if (cleanLot.isBlank()) {
            return MaterialError.BlankBatchLotNumber.left()
        }

        val today = clinicClock.now().toLocalDate()
        if (material.manufacturingDate.isAfter(today)) {
            return MaterialError.ManufacturingDateInFuture.left()
        }

        if (!material.expirationDate.isAfter(material.manufacturingDate)) {
            return MaterialError.InvalidDates.left()
        }

        val cleanedMaterial = material.copy(
            name = cleanName,
            invimaRegistration = cleanInvima,
            batchLotNumber = cleanLot
        )

        return materialRepository.saveMaterial(cleanedMaterial).fold(
            onSuccess = { it.right() },
            onFailure = { error ->
                val msg = error.message.orEmpty()
                if (msg.contains("23505") ||
                    msg.contains("duplicate key", ignoreCase = true) ||
                    msg.contains("unique", ignoreCase = true) ||
                    msg.contains("ux_clinical_materials", ignoreCase = true)
                ) {
                    MaterialError.DuplicateMaterial.left()
                } else {
                    MaterialError.PersistenceError(msg.ifBlank { "Error al persistir el material clínico." }).left()
                }
            }
        )
    }
}
