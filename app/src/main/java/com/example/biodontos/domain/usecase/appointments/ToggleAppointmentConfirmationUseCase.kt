package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.repository.AppointmentRepository

class ToggleAppointmentConfirmationUseCase(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentId: String, isConfirmed: Boolean): Result<Unit> {
        if (appointmentId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador de la cita no puede estar vacío"))
        }
        return appointmentRepository.updateConfirmationStatus(appointmentId, isConfirmed)
    }
}
