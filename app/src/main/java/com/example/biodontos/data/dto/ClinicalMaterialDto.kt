package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClinicalMaterialDto(
    val id: String? = null,
    val name: String,
    @SerialName("invima_registration") val invimaRegistration: String,
    @SerialName("batch_lot_number") val batchLotNumber: String,
    @SerialName("manufacturing_date") val manufacturingDate: String,
    @SerialName("expiration_date") val expirationDate: String,
    @SerialName("current_stock") val currentStock: Int = 0,
    @SerialName("unit_of_measure") val unitOfMeasure: String = "UNIDAD"
)
