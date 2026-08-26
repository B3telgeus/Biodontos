package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvolutionDto(
    val id: String? = null,
    @SerialName("record_id") val recordId: String,
    val description: String,
    @SerialName("doctor_name") val doctorName: String? = null,
    val date: String? = null
)
