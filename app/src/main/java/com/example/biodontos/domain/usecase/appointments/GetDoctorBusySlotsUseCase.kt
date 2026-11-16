package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.repository.AppointmentRepository

class GetDoctorBusySlotsUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(doctorId: String, date: String): Result<List<String>> {
        if (doctorId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador del doctor no puede estar vacío"))
        }
        return repository.getDoctorBusySlots(doctorId, date)
    }
}
