package com.example.biodontos.domain.usecase.appointment

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.repository.AppointmentRepository

class GetPatientAppointmentsUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(patientId: String): Result<List<Appointment>> {
        return repository.getAppointmentsByPatientId(patientId)
    }
}
