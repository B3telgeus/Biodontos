package com.example.biodontos.domain.model

data class Appointment(
    val id: String = "",
    val patientId: String,
    val doctorId: String? = null,
    val originEvolutionId: String? = null,
    val startTime: String,
    val endTime: String? = null,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val isConfirmed: Boolean = false,
    val reason: String? = null
)

enum class AppointmentStatus {
    SCHEDULED, CANCELLED, COMPLETED
}
