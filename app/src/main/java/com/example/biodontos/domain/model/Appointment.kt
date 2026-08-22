package com.example.biodontos.domain.model

data class Appointment(
    val id: String,
    val patientId: String,
    val startTime: String,
    val status: AppointmentStatus,
    val isConfirmed: Boolean
)

enum class AppointmentStatus {
    SCHEDULED, CANCELLED, COMPLETED
}
