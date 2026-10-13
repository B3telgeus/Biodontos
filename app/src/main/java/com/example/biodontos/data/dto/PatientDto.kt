package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val id: String? = null,
    @SerialName("dni_cedula") val dniCedula: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("consentimiento_firmado") val consentSigned: Boolean? = false,
    @SerialName("critical_allergies") val hasCriticalAllergies: Boolean? = false,
    @SerialName("allergies_description") val allergiesDescription: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val address: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val phone: String? = null,
    val email: String? = null
)