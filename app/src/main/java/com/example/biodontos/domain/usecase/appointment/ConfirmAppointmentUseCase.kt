package com.example.biodontos.domain.usecase.appointment

import com.example.biodontos.domain.repository.AppointmentRepository

class ConfirmAppointmentUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentId: String): Result<Unit> {
        return repository.updateConfirmationStatus(appointmentId, isConfirmed = true)
    }
}
