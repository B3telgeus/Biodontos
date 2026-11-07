package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId

fun DoctorDto.toDomain(): Doctor = Doctor(
    id = DoctorId(id?.ifEmpty { null } ?: userId ?: ""),
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    specialty = specialty,
    licenseNumber = licenseNumber,
    email = email,
    phone = phone,
    bio = bio,
    photoUrl = photoUrl,
    clinicAddress = clinicAddress,
    services = services
)

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
    photoUrl = photoUrl,
    clinicAddress = clinicAddress,
    services = services
)
