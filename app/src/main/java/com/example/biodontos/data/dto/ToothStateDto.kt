package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToothStateDto(
    val id: String? = null,
    @SerialName("evolution_id") val evolutionId: String,
    @SerialName("tooth_number") val toothNumber: Int,
    val surface: String,
    val condition: String,
    @SerialName("created_at") val createdAt: String? = null
)
