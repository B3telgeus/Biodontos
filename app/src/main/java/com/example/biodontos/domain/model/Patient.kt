package com.example.biodontos.domain.model

data class Patient(
    val id: String,
    val dniCedula: String,
    val firstName: String,
    val lastName: String,
    val consentSigned: Boolean,
    val hasCriticalAllergies: Boolean
)
