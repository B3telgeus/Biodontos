package com.example.biodontos.domain.usecase.appointment

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.repository.AppointmentRepository

class GetDoctorAppointmentsUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(doctorId: String): Result<List<Appointment>> {
        return repository.getAppointmentsByDoctorId(doctorId)
    }
}
