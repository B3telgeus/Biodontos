package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val id: String? = null,
    @SerialName("dni_cedula") val dniCedula: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("critical_allergies") val criticalAllergies: String = "Ninguna",
    val phone: String? = null,
    val email: String? = null,
    @SerialName("consent_signed") val consentSigned: Boolean = false,
    val address: String? = null,
    @SerialName("birth_date") val birthDate: String? = null
)
