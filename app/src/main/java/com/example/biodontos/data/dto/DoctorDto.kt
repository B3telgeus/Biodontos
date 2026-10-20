package com.example.biodontos.data.dto

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String = "",
    val phone: String? = null,
    @SerialName("license_number") val licenseNumber: String = "",
    val specialty: String = "Odontología General",
    val bio: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null
)

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
    photoUrl = photoUrl
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
    photoUrl = photoUrl
)
