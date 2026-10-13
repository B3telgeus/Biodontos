package com.example.biodontos.domain.model

import java.time.LocalDateTime
import java.time.OffsetDateTime

enum class AppointmentStatus {
    SCHEDULED,
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}

data class Appointment(
    val id: String = "",
    val patientId: String,
    val doctorId: String? = null,
    val originEvolutionId: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val isConfirmed: Boolean = false,
    val reason: String? = null,
    val doctorName: String? = null
) {
    companion object {
        fun parseDateTime(dateTimeStr: String): LocalDateTime {
            return runCatching {
                OffsetDateTime.parse(dateTimeStr).toLocalDateTime()
            }.recoverCatching {
                LocalDateTime.parse(dateTimeStr)
            }.getOrDefault(LocalDateTime.now())
        }
    }
}
