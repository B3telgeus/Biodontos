package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.AppointmentDto
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import java.time.format.DateTimeFormatter

fun AppointmentDto.toDomain(): Appointment {
    val parsedStatus = when {
        status.equals("CANCELLED", ignoreCase = true) -> AppointmentStatus.CANCELLED
        status.equals("COMPLETED", ignoreCase = true) -> AppointmentStatus.COMPLETED
        isConfirmed -> AppointmentStatus.CONFIRMED
        else -> AppointmentStatus.PENDING
    }

    return Appointment(
        id = id ?: "",
        patientId = patientId,
        doctorId = doctorId,
        originEvolutionId = originEvolutionId,
        startTime = Appointment.parseDateTime(startTime),
        endTime = endTime?.let { Appointment.parseDateTime(it) },
        status = parsedStatus,
        isConfirmed = isConfirmed,
        reason = reason
    )
}

fun Appointment.toDto(): AppointmentDto {
    val statusStr = when (status) {
        AppointmentStatus.CANCELLED -> "CANCELLED"
        AppointmentStatus.COMPLETED -> "COMPLETED"
        else -> "SCHEDULED"
    }

    return AppointmentDto(
        id = id.trim().ifEmpty { null },
        patientId = patientId.trim(),
        doctorId = doctorId?.trim()?.ifEmpty { null },
        originEvolutionId = originEvolutionId?.trim()?.ifEmpty { null },
        startTime = startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        endTime = endTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        status = statusStr,
        isConfirmed = isConfirmed || status == AppointmentStatus.CONFIRMED,
        reason = reason?.trim()?.ifEmpty { null }
    )
}
