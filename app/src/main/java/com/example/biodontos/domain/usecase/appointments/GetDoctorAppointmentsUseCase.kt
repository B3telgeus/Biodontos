package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.repository.AppointmentRepository

class GetDoctorAppointmentsUseCase(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(doctorId: String): Result<List<Appointment>> {
        if (doctorId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador del doctor no puede estar vacío"))
        }
        return appointmentRepository.getAppointmentsByDoctorId(doctorId)
    }
}
