package com.example.biodontos.domain.model

data class EvolutionMaterialUsed(
    val id: String = "",
    val materialId: String,
    val materialName: String,
    val quantityUsed: Double = 1.0,
    val lotNumberSnapshot: String,
    val invimaSnapshot: String,
    val expirationDateSnapshot: String
)

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
    val materialsUsed: List<EvolutionMaterialUsed> = emptyList()
)
