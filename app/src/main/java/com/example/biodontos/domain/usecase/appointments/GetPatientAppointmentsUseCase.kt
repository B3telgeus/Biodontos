package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.repository.AppointmentRepository

class GetPatientAppointmentsUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(patientId: String): Result<List<Appointment>> {
        if (patientId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador del paciente no puede estar vacío"))
        }
        return repository.getAppointmentsByPatientId(patientId)
    }
}
