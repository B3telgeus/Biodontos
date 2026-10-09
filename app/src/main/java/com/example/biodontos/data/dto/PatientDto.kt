package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PatientDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("dni_cedula") val dniCedula: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("first_last_name") val firstLastName: String = "",
    @SerialName("second_last_name") val secondLastName: String = "",
    @SerialName("document_type_id") val documentTypeId: Int = 1,
    @SerialName("birth_date") val birthDate: String = "",
    @SerialName("birth_place") val birthPlace: String = "No especificado",
    val eps: String = "Particular",
    val occupation: String? = null,
    @SerialName("marital_status") val maritalStatus: String? = null,
    @SerialName("blood_group") val bloodGroup: String = "O",
    @SerialName("rh_factor") val rhFactor: String = "+",
    val phone: String = "",
    val email: String? = null,
    val address: String? = null,
    @SerialName("guardian_relationship") val guardianRelationship: String? = null,
    @SerialName("guardian_first_name") val guardianFirstName: String? = null,
    @SerialName("guardian_last_name") val guardianLastName: String? = null,
    @SerialName("guardian_phone") val guardianPhone: String? = null,
    @SerialName("guardian_address") val guardianAddress: String? = null,
    @SerialName("last_dental_visit") val lastDentalVisit: String? = null,
    @SerialName("consultation_reason") val consultationReason: String? = null,
    @SerialName("current_consultation_reason") val currentConsultationReason: String? = null,
    @SerialName("illness_history") val illnessHistory: String? = null,
    @SerialName("critical_allergies") val criticalAllergies: String? = "Ninguna",
    @SerialName("allergies_description") val allergiesDescription: String? = null,
    @SerialName("consent_signed") val consentSigned: Boolean = false
) {
    constructor(
        id: String? = null,
        dniCedula: String,
        firstName: String,
        lastName: String,
        criticalAllergies: String = "Ninguna",
        phone: String? = null,
        email: String? = null,
        consentSigned: Boolean = false,
        address: String? = null,
        birthDate: String? = null
    ) : this(
        id = id,
        dniCedula = dniCedula,
        firstName = firstName,
        firstLastName = lastName.trim().split(" ").firstOrNull() ?: lastName,
        secondLastName = lastName.trim().split(" ").drop(1).joinToString(" "),
        criticalAllergies = criticalAllergies,
        phone = phone ?: "",
        email = email,
        consentSigned = consentSigned,
        address = address,
        birthDate = birthDate ?: ""
    )

    val lastName: String get() = if (secondLastName.isBlank()) firstLastName else "$firstLastName $secondLastName"
}
