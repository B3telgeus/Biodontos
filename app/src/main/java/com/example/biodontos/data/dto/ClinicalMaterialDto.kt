package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClinicalMaterialDto(
    val id: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    val name: String,
    @SerialName("invima_registration") val invimaRegistration: String,
    @SerialName("batch_lot_number") val batchLotNumber: String,
    @SerialName("manufacturing_date") val manufacturingDate: String,
    @SerialName("expiration_date") val expirationDate: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
