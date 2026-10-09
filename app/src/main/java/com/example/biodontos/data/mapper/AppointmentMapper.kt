package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.AppointmentDto
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus

fun AppointmentDto.toDomain(): Appointment = Appointment(
    id = id ?: "",
    patientId = patientId,
    doctorId = doctorId,
    originEvolutionId = originEvolutionId,
    startTime = startTime,
    endTime = endTime,
    status = runCatching { AppointmentStatus.valueOf(status) }.getOrDefault(AppointmentStatus.SCHEDULED),
    isConfirmed = isConfirmed,
    reason = reason
)

fun Appointment.toDto(): AppointmentDto = AppointmentDto(
    id = id.ifEmpty { null },
    patientId = patientId,
    doctorId = doctorId,
    originEvolutionId = originEvolutionId,
    startTime = startTime,
    endTime = endTime,
    status = status.name,
    isConfirmed = isConfirmed,
    reason = reason
)
