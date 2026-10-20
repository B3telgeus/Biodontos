package com.example.biodontos.domain.usecase.appointment

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.repository.AppointmentRepository

class BookAppointmentUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(appointment: Appointment): Result<Appointment> {
        return repository.createAppointment(appointment)
    }
}
