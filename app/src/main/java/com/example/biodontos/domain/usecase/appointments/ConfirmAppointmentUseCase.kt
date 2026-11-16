package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.repository.AppointmentRepository

class ConfirmAppointmentUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentId: String): Result<Unit> {
        if (appointmentId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador de la cita no puede estar vacío"))
        }
        return repository.updateConfirmationStatus(appointmentId, isConfirmed = true)
    }
}
