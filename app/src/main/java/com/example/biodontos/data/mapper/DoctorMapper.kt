package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.domain.model.Doctor

fun DoctorDto.toDomain(): Doctor = Doctor(
    id = id ?: userId ?: "",
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    specialty = specialty,
    email = email,
    phone = phone,
    licenseNumber = licenseNumber
)

fun Doctor.toDto(): DoctorDto = DoctorDto(
    id = id.ifEmpty { null },
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    specialty = specialty,
    email = email,
    phone = phone,
    licenseNumber = licenseNumber
)
