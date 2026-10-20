package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId

fun DoctorDto.toDomain(): Doctor = com.example.biodontos.data.dto.DoctorDto(
    id = id,
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phone = phone,
    licenseNumber = licenseNumber,
    specialty = specialty,
    bio = bio,
    photoUrl = photoUrl
).let {
    Doctor(
        id = DoctorId(it.id?.ifEmpty { null } ?: it.userId ?: ""),
        userId = it.userId,
        firstName = it.firstName,
        lastName = it.lastName,
        specialty = it.specialty,
        licenseNumber = it.licenseNumber,
        email = it.email,
        phone = it.phone,
        bio = it.bio,
        photoUrl = it.photoUrl
    )
}

fun Doctor.toDto(): DoctorDto = DoctorDto(
    id = id.value.ifEmpty { null },
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    specialty = specialty,
    licenseNumber = licenseNumber,
    email = email,
    phone = phone,
    bio = bio,
    photoUrl = photoUrl
)
