package com.example.biodontos.data.dto

import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class DoctorAvailabilityDto(
    val id: String? = null,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("slot_minutes") val slotMinutes: Int = 30,
    @SerialName("is_active") val isActive: Boolean = true
)

fun DoctorAvailabilityDto.toDomain(): WeeklyAvailability {
    val day = runCatching { DayOfWeek.of(dayOfWeek) }.getOrDefault(DayOfWeek.MONDAY)
    val parsedStart = runCatching { LocalTime.parse(startTime.take(8)) }.getOrDefault(LocalTime.of(8, 0))
    val parsedEnd = runCatching { LocalTime.parse(endTime.take(8)) }.getOrDefault(LocalTime.of(12, 0))

    return WeeklyAvailability(
        id = id ?: "",
        doctorId = DoctorId(doctorId),
        dayOfWeek = day,
        startTime = parsedStart,
        endTime = parsedEnd,
        slotMinutes = slotMinutes,
        isActive = isActive
    )
}

fun WeeklyAvailability.toDto(): DoctorAvailabilityDto {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return DoctorAvailabilityDto(
        id = id.ifBlank { null },
        doctorId = doctorId.value,
        dayOfWeek = dayOfWeek.value,
        startTime = startTime.format(timeFormatter),
        endTime = endTime.format(timeFormatter),
        slotMinutes = slotMinutes,
        isActive = isActive
    )
}

