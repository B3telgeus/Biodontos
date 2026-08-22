package com.example.biodontos.domain.usecase.notifications

import com.example.biodontos.domain.repository.AppointmentRepository

class ConfirmAttendance1ClickUseCase(private val repository: AppointmentRepository) {
    suspend operator fun invoke(appointmentId: String): Result<Unit> {
        return repository.updateConfirmationStatus(appointmentId, true)
    }
}
