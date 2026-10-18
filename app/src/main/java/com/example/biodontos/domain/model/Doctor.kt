package com.example.biodontos.domain.model

data class Doctor(
    val id: String = "",
    val userId: String? = null,
    val firstName: String,
    val lastName: String,
    val specialty: String,
    val email: String,
    val phone: String? = null,
    val licenseNumber: String? = null
) {
    val fullName: String
        get() = "Dr(a). $firstName $lastName".trim()
}
