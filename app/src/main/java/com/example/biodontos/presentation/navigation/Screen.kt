package com.example.biodontos.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object PatientList

@Serializable
object PatientRegistration

@Serializable
data class Odontogram(val patientId: String)
