package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId

fun DoctorDto.toDomainModel(): Doctor = Doctor(
    id = DoctorId(id.ifEmpty { userId ?: "" }),
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    specialty = specialty,
    email = email,
    phone = phone,
    licenseNumber = licenseNumber,
    bio = bio,
    photoUrl = photoUrl
)
