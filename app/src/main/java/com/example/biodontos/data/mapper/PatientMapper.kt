package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.domain.model.Patient

fun PatientDto.toDomain(): Patient {
    return Patient(
        id = id ?: "",
        dniCedula = dniCedula ?: "",
        firstName = firstName ?: "",
        lastName = lastName ?: "",
        consentSigned = consentSigned ?: false,
        hasCriticalAllergies = !hasCriticalAllergies.isNullOrEmpty() && hasCriticalAllergies.lowercase() != "ninguna" && hasCriticalAllergies.lowercase() != "false"
    )
}

fun Patient.toDto(): PatientDto {
    return PatientDto(
        id = if (id.isEmpty()) null else id,
        dniCedula = dniCedula,
        firstName = firstName,
        lastName = lastName,
        consentSigned = consentSigned,
        hasCriticalAllergies = if (hasCriticalAllergies) "Sí" else "Ninguna"
    )
}