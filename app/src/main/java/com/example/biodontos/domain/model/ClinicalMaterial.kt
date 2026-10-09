package com.example.biodontos.domain.model

data class ClinicalMaterial(
    val id: String = "",
    val name: String,
    val invimaRegistration: String,
    val batchLotNumber: String,
    val manufacturingDate: String,
    val expirationDate: String,
    val currentStock: Int = 0,
    val unitOfMeasure: String = "UNIDAD"
)
