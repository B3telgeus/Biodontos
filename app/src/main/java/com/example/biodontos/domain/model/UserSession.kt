package com.example.biodontos.domain.model

enum class UserRole {
    DOCTOR,
    PATIENT,
    UNKNOWN
}

data class UserSession(
    val id: String,
    val email: String,
    val role: UserRole,
    val displayName: String = ""
)
