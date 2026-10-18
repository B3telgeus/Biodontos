package com.example.biodontos.domain.usecase.appointment

import com.example.biodontos.domain.repository.AppointmentRepository

class RescheduleAppointmentUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(
        appointmentId: String,
        newStartTime: String,
        newEndTime: String? = null
    ): Result<Unit> {
        return repository.rescheduleAppointment(appointmentId, newStartTime, newEndTime)
    }
}
