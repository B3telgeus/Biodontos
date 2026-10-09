package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("origin_evolution_id") val originEvolutionId: String? = null,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String? = null,
    val status: String = "SCHEDULED",
    @SerialName("confirmation_status") val isConfirmed: Boolean = false,
    val reason: String? = null
)
