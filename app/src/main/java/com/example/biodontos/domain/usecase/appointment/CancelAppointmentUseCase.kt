package com.example.biodontos.domain.usecase.appointment

import com.example.biodontos.domain.repository.AppointmentRepository

class CancelAppointmentUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentId: String, reason: String? = null): Result<Unit> {
        return repository.cancelAppointment(appointmentId, reason)
    }
}
