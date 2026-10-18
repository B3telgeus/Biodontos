package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val specialty: String = "Odontología General",
    val email: String = "",
    val phone: String? = null,
    @SerialName("license_number") val licenseNumber: String? = null
)
