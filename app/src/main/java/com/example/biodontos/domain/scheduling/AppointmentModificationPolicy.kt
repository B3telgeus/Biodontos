package com.example.biodontos.domain.scheduling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.model.AppointmentStatus
import java.time.Duration
import java.time.LocalDateTime

class AppointmentModificationPolicy(
    private val minHoursBefore: Long = 2
) {
    fun canModify(appointment: Appointment, now: LocalDateTime): Either<AppointmentError, Unit> {
        if (appointment.status == AppointmentStatus.CANCELLED) {
            return AppointmentError.AlreadyCancelled.left()
        }

        if (appointment.status == AppointmentStatus.COMPLETED) {
            return AppointmentError.ModificationWindowClosed(minHoursBefore).left()
        }

        val hoursUntilAppointment = Duration.between(now, appointment.startTime).toHours()
        if (hoursUntilAppointment < minHoursBefore) {
            return AppointmentError.ModificationWindowClosed(minHoursBefore).left()
        }

        return Unit.right()
    }
}
