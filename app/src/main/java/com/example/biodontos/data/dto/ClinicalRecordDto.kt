package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClinicalRecordDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String,
    @SerialName("dentition_type") val dentitionType: String = "ADULT"
)
