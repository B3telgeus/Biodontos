package com.example.biodontos.domain.usecase.appointments

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.scheduling.AppointmentModificationPolicy
import com.example.biodontos.domain.scheduling.ClinicClock
import java.time.LocalDateTime

class RescheduleAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val modificationPolicy: AppointmentModificationPolicy,
    private val clinicClock: ClinicClock
) {
    suspend operator fun invoke(
        appointmentId: String,
        newStartTime: LocalDateTime,
        newEndTime: LocalDateTime? = null
    ): Either<AppointmentError, Unit> {
        val now = clinicClock.now()

        val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
        val appointment = appointmentResult.getOrNull()
            ?: return AppointmentError.NotOwner.left()

        val canModifyResult = modificationPolicy.canModify(appointment, now)
        if (canModifyResult.isLeft()) {
            return canModifyResult
        }

        if (newStartTime.isBefore(now.plusHours(2))) {
            return AppointmentError.ModificationWindowClosed(2).left()
        }

        val effectiveEndTime = newEndTime ?: newStartTime.plusMinutes(30)

        val updateResult = appointmentRepository.rescheduleAppointment(
            appointmentId = appointmentId,
            newStartTime = newStartTime,
            newEndTime = effectiveEndTime
        )

        return updateResult.fold(
            onSuccess = { Unit.right() },
            onFailure = { AppointmentError.SlotUnavailable.left() }
        )
    }
}
