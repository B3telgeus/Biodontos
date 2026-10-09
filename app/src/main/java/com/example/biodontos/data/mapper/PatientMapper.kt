package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.domain.model.Patient

fun PatientDto.toDomain(): Patient {
    val allergies = (criticalAllergies ?: allergiesDescription ?: "Ninguna").ifBlank { "Ninguna" }
    val hasAllergies = !allergies.equals("Ninguna", ignoreCase = true) &&
            !allergies.equals("No", ignoreCase = true) &&
            !allergies.equals("false", ignoreCase = true)

    return Patient(
        id = id ?: "",
        dniCedula = dniCedula,
        firstName = firstName,
        firstLastName = firstLastName,
        secondLastName = secondLastName,
        documentTypeId = documentTypeId,
        birthDate = birthDate,
        birthPlace = birthPlace,
        eps = eps,
        occupation = occupation,
        maritalStatus = maritalStatus,
        bloodGroup = bloodGroup,
        rhFactor = rhFactor,
        phone = phone,
        email = email,
        address = address,
        guardianRelationship = guardianRelationship,
        guardianFirstName = guardianFirstName,
        guardianLastName = guardianLastName,
        guardianPhone = guardianPhone,
        guardianAddress = guardianAddress,
        lastDentalVisit = lastDentalVisit,
        consultationReason = consultationReason,
        currentConsultationReason = currentConsultationReason,
        illnessHistory = illnessHistory,
        consentSigned = consentSigned,
        hasCriticalAllergies = hasAllergies,
        allergiesDescription = allergies
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
        id = id.ifEmpty { null },
        dniCedula = dniCedula,
        firstName = firstName,
        firstLastName = firstLastName,
        secondLastName = secondLastName,
        documentTypeId = documentTypeId,
        birthDate = birthDate,
        birthPlace = birthPlace,
        eps = eps,
        occupation = occupation,
        maritalStatus = maritalStatus,
        bloodGroup = bloodGroup,
        rhFactor = rhFactor,
        phone = phone,
        email = email,
        address = address,
        guardianRelationship = guardianRelationship,
        guardianFirstName = guardianFirstName,
        guardianLastName = guardianLastName,
        guardianPhone = guardianPhone,
        guardianAddress = guardianAddress,
        lastDentalVisit = lastDentalVisit,
        consultationReason = consultationReason,
        currentConsultationReason = currentConsultationReason,
        illnessHistory = illnessHistory,
        criticalAllergies = allergyText,
        allergiesDescription = allergyText,
        consentSigned = consentSigned
    )
}
