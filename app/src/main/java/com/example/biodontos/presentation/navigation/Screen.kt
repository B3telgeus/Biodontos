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
object DoctorMaterials

@Serializable
object DoctorProfile

@Serializable
object PatientRegistration

@Serializable
data class Odontogram(val patientId: String)

@Serializable
data class PatientDetail(
    val patientId: String,
    val initialTab: String = "HISTORY"
)

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
data class AppointmentConfirmation(
    val appointmentId: String,
    val doctorId: String,
    val doctorName: String,
    val specialty: String,
    val startIso: String,
    val roomOrLocation: String = "Sede Principal - Consultorio 102",
    val reason: String = "Consulta General",
    val instructions: String = "Presentarse 10 minutos antes. Traer documento de identidad y cepillo dental.",
    val isRescheduled: Boolean = false
)

@Serializable
object PatientPayments

