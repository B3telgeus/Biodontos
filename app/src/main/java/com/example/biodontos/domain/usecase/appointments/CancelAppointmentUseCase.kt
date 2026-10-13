package com.example.biodontos.domain.usecase.appointments

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.scheduling.AppointmentModificationPolicy
import com.example.biodontos.domain.scheduling.ClinicClock

class CancelAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val modificationPolicy: AppointmentModificationPolicy,
    private val clinicClock: ClinicClock
) {
    suspend operator fun invoke(
        appointmentId: String
    ): Either<AppointmentError, Unit> {
        val now = clinicClock.now()

        val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
        val appointment = appointmentResult.getOrNull()
            ?: return AppointmentError.NotOwner.left()

        val canModifyResult = modificationPolicy.canModify(appointment, now)
        if (canModifyResult.isLeft()) {
            return canModifyResult
        }

        val cancelResult = appointmentRepository.cancelAppointment(appointmentId)

        return cancelResult.fold(
            onSuccess = { Unit.right() },
            onFailure = { AppointmentError.AlreadyCancelled.left() }
        )
    }
}
