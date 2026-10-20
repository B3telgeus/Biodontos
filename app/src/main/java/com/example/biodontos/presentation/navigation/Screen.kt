package com.example.biodontos.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object Splash

@Serializable
object SessionGate

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
object SignUpSuccess

@Serializable
object ForgotPassword

// --- Portal de Doctores ---
@Serializable
object DoctorHome

@Serializable
object PatientList

@Serializable
object DoctorAppointments

@Serializable
object DoctorDocuments

@Serializable
object DoctorProfile

@Serializable
object PatientRegistration

@Serializable
data class Odontogram(val patientId: String)

@Serializable
data class PatientDetail(val patientId: String)

@Serializable
data class ClinicalAdmission(val patientId: String? = null)

// --- Portal de Pacientes ---
@Serializable
object PatientHome

@Serializable
object MyAppointments

@Serializable
object DoctorSearch

@Serializable
data class DoctorDetail(val doctorId: String)

@Serializable
data class BookAppointment(val doctorId: String, val rescheduleAppointmentId: String? = null)

@Serializable
object PatientPayments
