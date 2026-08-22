package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String,
    @SerialName("start_time") val startTime: String,
    val status: String,
    @SerialName("confirmation_status") val isConfirmed: Boolean = false
)
