package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.DoctorRepository

class GetDoctorAvailabilityUseCase(
    private val doctorRepository: DoctorRepository
) {
    suspend operator fun invoke(doctorId: String): Result<List<WeeklyAvailability>> {
        if (doctorId.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID del doctor no puede estar vacío"))
        }
        return doctorRepository.getDoctorAvailability(doctorId)
    }
}
