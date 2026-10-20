package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.domain.model.Patient

fun PatientDto.toDomain(): Patient {
    val allergies = (criticalAllergies ?: allergiesDescription ?: "Ninguna").ifBlank { "Ninguna" }
    val hasAllergies = !allergies.equals("Ninguna", ignoreCase = true) &&
            !allergies.equals("No", ignoreCase = true) &&
            !allergies.equals("false", ignoreCase = true)

    val effectiveFirstLastName = if (firstLastName.isNotBlank()) {
        firstLastName
    } else {
        lastNameCol?.trim()?.split(" ")?.firstOrNull().orEmpty()
    }

    val effectiveSecondLastName = if (secondLastName.isNotBlank()) {
        secondLastName
    } else {
        lastNameCol?.trim()?.split(" ")?.drop(1)?.joinToString(" ").orEmpty()
    }

    return Patient(
        id = id ?: "",
        userId = userId,
        dniCedula = dniCedula,
        firstName = firstName,
        firstLastName = effectiveFirstLastName,
        secondLastName = effectiveSecondLastName,
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
        locality = locality,
        guardianId = guardianId,
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

    val resolvedId = if (id.isNotBlank()) id else Patient.generateIdFromDni(dniCedula)

    return PatientDto(
        id = resolvedId.trim().ifEmpty { null },
        userId = userId?.trim()?.ifEmpty { null },
        dniCedula = dniCedula.trim(),
        firstName = firstName,
        lastNameCol = fullLastName.ifBlank { firstLastName },
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
        locality = locality,
        guardianId = guardianId,
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
