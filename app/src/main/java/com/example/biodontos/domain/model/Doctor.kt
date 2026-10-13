package com.example.biodontos.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class Doctor(
    val id: DoctorId,
    val userId: String? = null,
    val firstName: String,
    val lastName: String,
    val specialty: String,
    val licenseNumber: String,
    val email: String,
    val phone: String? = null,
    val bio: String? = null,
    val photoUrl: String? = null
) {
    val displayName: String
        get() = "Dr. $firstName $lastName".trim()
}

data class WeeklyAvailability(
    val id: String = "",
    val doctorId: DoctorId,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val slotMinutes: Int = 30,
    val isActive: Boolean = true
) {
    init {
        require(endTime.isAfter(startTime)) { "La hora de fin debe ser posterior a la de inicio" }
        require(slotMinutes in 10..240) { "La duración del slot debe estar entre 10 y 240 minutos" }
    }
}
