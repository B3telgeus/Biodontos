package com.example.biodontos.domain.model

data class Guardian(
    val id: String = "",
    val firstName: String,
    val lastName: String,
    val phone: String = "",
    val address: String = "",
    val locality: String = ""
) {
    val fullName: String get() = "$firstName $lastName".trim()
}
