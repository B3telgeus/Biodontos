package com.example.biodontos.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object Splash

@Serializable
object Onboarding

@Serializable
object UserTypeSelection

@Serializable
object Login

@Serializable
object DoctorRegistration

@Serializable
object PatientSignUp

@Serializable
object ForgotPassword

@Serializable
object PatientList

@Serializable
object DoctorProfile

@Serializable
object PatientHome

@Serializable
object PatientRegistration

@Serializable
data class Odontogram(val patientId: String)

@Serializable
data class ClinicalAdmission(val patientId: String? = null)
