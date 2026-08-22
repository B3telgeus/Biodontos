package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val id: String? = null,
    @SerialName("dni_cedula") val dniCedula: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("consentimiento_firmado") val consentSigned: Boolean = false,
    @SerialName("tiene_alergias_criticas") val hasCriticalAllergies: Boolean = false
)
