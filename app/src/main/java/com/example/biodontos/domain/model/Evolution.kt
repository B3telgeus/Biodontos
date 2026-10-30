package com.example.biodontos.domain.model

import com.example.biodontos.domain.model.history.VoidInfo
import java.time.LocalDate

data class EvolutionMaterialUsed(
    val id: String = "",
    val materialId: String,
    val materialName: String,
    val quantityUsed: Double = 1.0,
    val lotNumberSnapshot: String,
    val invimaSnapshot: String,
    val expirationDateSnapshot: String,
    val materialNameSnapshot: String = materialName,
    val manufacturingDateSnapshot: String = ""
) {
    constructor(
        id: String = "",
        materialId: MaterialId,
        materialName: String,
        quantityUsed: Double = 1.0,
        lotNumberSnapshot: String,
        invimaSnapshot: String,
        expirationDateSnapshot: LocalDate,
        materialNameSnapshot: String = materialName,
        manufacturingDateSnapshot: String = ""
    ) : this(
        id = id,
        materialId = materialId.value,
        materialName = materialName,
        quantityUsed = quantityUsed,
        lotNumberSnapshot = lotNumberSnapshot,
        invimaSnapshot = invimaSnapshot,
        expirationDateSnapshot = expirationDateSnapshot.toString(),
        materialNameSnapshot = materialNameSnapshot,
        manufacturingDateSnapshot = manufacturingDateSnapshot
    )
}

data class Evolution(
    val id: String = "",
    val recordId: String,
    val doctorId: String,
    val date: String,
    val procedureTime: String,
    val description: String,
    val doctorName: String? = null,
    val toothTreated: Int? = null,
    val cost: Double = 0.0,
    val paymentAmount: Double = 0.0,
    val balance: Double = (cost - paymentAmount).coerceAtLeast(0.0),
    val patientSignatureUrl: String? = null,
    val doctorSignatureUrl: String? = null,
    val materialsUsed: List<EvolutionMaterialUsed> = emptyList(),
    val voidInfo: VoidInfo? = null,
    val amendsEvolutionId: String? = null
) {
    val isVoided: Boolean get() = voidInfo != null
    val isClarification: Boolean get() = !amendsEvolutionId.isNullOrBlank()
}
