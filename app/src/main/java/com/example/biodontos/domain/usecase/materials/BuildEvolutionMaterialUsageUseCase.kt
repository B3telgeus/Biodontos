package com.example.biodontos.domain.usecase.materials

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.MaterialError
import com.example.biodontos.domain.scheduling.ClinicClock
import java.time.LocalDate

class BuildEvolutionMaterialUsageUseCase(
    private val clinicClock: ClinicClock
) {
    suspend operator fun invoke(
        material: ClinicalMaterial,
        quantityUsed: Double = 1.0,
        careDate: LocalDate = clinicClock.now().toLocalDate()
    ): Either<MaterialError, EvolutionMaterialUsed> {
        if (!material.isActive) {
            return MaterialError.MaterialInactive.left()
        }

        if (quantityUsed <= 0.0) {
            return MaterialError.InvalidQuantity.left()
        }

        if (material.isExpired(referenceDate = careDate)) {
            return MaterialError.MaterialExpired.left()
        }

        return EvolutionMaterialUsed(
            id = "",
            materialId = material.id.value,
            materialName = material.name,
            quantityUsed = quantityUsed,
            lotNumberSnapshot = material.batchLotNumber,
            invimaSnapshot = material.invimaRegistration,
            expirationDateSnapshot = material.expirationDate.toString(),
            materialNameSnapshot = material.name,
            manufacturingDateSnapshot = material.manufacturingDate.toString()
        ).right()
    }
}
