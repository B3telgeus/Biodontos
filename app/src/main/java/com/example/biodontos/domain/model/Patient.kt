package com.example.biodontos.domain.model

data class Patient(
    val id: String = "",
    val dniCedula: String,
    val firstName: String,
    val firstLastName: String = "",
    val secondLastName: String = "",
    val documentTypeId: Int = 1,
    val birthDate: String = "",
    val birthPlace: String = "No especificado",
    val eps: String = "Particular",
    val occupation: String? = null,
    val maritalStatus: String? = null,
    val bloodGroup: String = "O",
    val rhFactor: String = "+",
    val phone: String = "",
    val email: String? = null,
    val address: String? = null,
    // Acudiente / Representante
    val guardianRelationship: String? = null,
    val guardianFirstName: String? = null,
    val guardianLastName: String? = null,
    val guardianPhone: String? = null,
    val guardianAddress: String? = null,
    // Antecedentes
    val lastDentalVisit: String? = null,
    val consultationReason: String? = null,
    val currentConsultationReason: String? = null,
    val illnessHistory: String? = null,
    // Clínico básico
    val consentSigned: Boolean = false,
    val hasCriticalAllergies: Boolean = false,
    val allergiesDescription: String = if (hasCriticalAllergies) "Alergias presentes" else "Ninguna"
) {
    constructor(
        id: String = "",
        dniCedula: String,
        firstName: String,
        lastName: String,
        consentSigned: Boolean = false,
        hasCriticalAllergies: Boolean = false,
        allergiesDescription: String = if (hasCriticalAllergies) "Alergias presentes" else "Ninguna",
        phone: String? = null,
        email: String? = null
    ) : this(
        id = id,
        dniCedula = dniCedula,
        firstName = firstName,
        firstLastName = lastName.trim().split(" ").firstOrNull() ?: lastName,
        secondLastName = lastName.trim().split(" ").drop(1).joinToString(" "),
        consentSigned = consentSigned,
        hasCriticalAllergies = hasCriticalAllergies,
        allergiesDescription = allergiesDescription,
        phone = phone ?: "",
        email = email
    )

    val lastName: String get() = if (secondLastName.isBlank()) firstLastName else "$firstLastName $secondLastName"
    val fullName: String get() = "$firstName $firstLastName $secondLastName".trim().replace("\\s+".toRegex(), " ")
    val fullLastName: String get() = "$firstLastName $secondLastName".trim().replace("\\s+".toRegex(), " ")

    companion object {
        fun generateIdFromDni(dni: String): String {
            val cleaned = dni.trim()
            return if (cleaned.isNotEmpty()) {
                java.util.UUID.nameUUIDFromBytes("patient_$cleaned".toByteArray(java.nio.charset.StandardCharsets.UTF_8)).toString()
            } else {
                java.util.UUID.randomUUID().toString()
            }
        }
    }
}
