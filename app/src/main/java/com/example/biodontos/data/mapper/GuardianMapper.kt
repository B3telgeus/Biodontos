package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.GuardianDto
import com.example.biodontos.domain.model.Guardian

fun GuardianDto.toDomain(): Guardian = Guardian(
    id = id ?: "",
    firstName = firstName,
    lastName = lastName,
    phone = phone ?: "",
    address = address ?: "",
    locality = locality ?: ""
)

fun Guardian.toDto(): GuardianDto {
    val resolvedId = id.ifBlank { null }
    return GuardianDto(
        id = resolvedId,
        firstName = firstName.trim(),
        lastName = lastName.trim(),
        phone = phone.trim().ifBlank { null },
        address = address.trim().ifBlank { null },
        locality = locality.trim().ifBlank { null }
    )
}
