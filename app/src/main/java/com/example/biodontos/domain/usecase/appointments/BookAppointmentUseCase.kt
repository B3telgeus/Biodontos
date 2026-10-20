package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import java.time.LocalDateTime

class BookAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val clinicClock: ClinicClock
) {
    suspend operator fun invoke(
        patientId: String,
        doctorId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime? = null,
        reason: String? = null
    ): Result<Appointment> {
        val now = clinicClock.now()
        if (startTime.isBefore(now.plusHours(2))) {
            return Result.failure(IllegalArgumentException("La cita debe programarse con al menos 2 horas de anticipación"))
        }

        val effectiveEndTime = endTime ?: startTime.plusMinutes(30)

        val appointment = Appointment(
            patientId = patientId,
            doctorId = doctorId,
            startTime = startTime,
            endTime = effectiveEndTime,
            status = AppointmentStatus.PENDING,
            isConfirmed = false,
            reason = reason?.trim()
        )

        return appointmentRepository.createAppointment(appointment)
    }
}
