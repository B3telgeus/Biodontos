package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.domain.model.Patient

fun PatientDto.toDomain(): Patient {
    val allergies = criticalAllergies.ifBlank { "Ninguna" }
    val hasAllergies = !allergies.equals("Ninguna", ignoreCase = true) &&
            !allergies.equals("No", ignoreCase = true) &&
            !allergies.equals("false", ignoreCase = true)

    return Patient(
        id = id ?: "",
        dniCedula = dniCedula,
        firstName = firstName,
        lastName = lastName,
        consentSigned = consentSigned,
        hasCriticalAllergies = hasAllergies,
        allergiesDescription = allergies,
        phone = phone,
        email = email
    )
}

fun Patient.toDto(): PatientDto {
    val allergyText = if (hasCriticalAllergies) {
        if (allergiesDescription.isNotBlank() && !allergiesDescription.equals("Ninguna", ignoreCase = true)) {
            allergiesDescription.trim()
        } else {
            "Alergias presentes"
        }
    } else {
        "Ninguna"
    }

    return PatientDto(
        id = if (id.isEmpty()) null else id,
        dniCedula = dniCedula,
        firstName = firstName,
        lastName = lastName,
        criticalAllergies = allergyText,
        phone = phone,
        email = email,
        consentSigned = consentSigned
    )
}
