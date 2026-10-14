package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.DoctorRepository

class SaveDoctorAvailabilityUseCase(
    private val doctorRepository: DoctorRepository
) {
    suspend operator fun invoke(
        doctorId: String,
        availabilities: List<WeeklyAvailability>
    ): Result<Unit> {
        if (doctorId.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID del doctor no puede estar vacío"))
        }

        for (avail in availabilities) {
            if (avail.isActive) {
                if (!avail.endTime.isAfter(avail.startTime)) {
                    return Result.failure(
                        IllegalArgumentException("La hora de fin debe ser posterior a la de inicio para ${avail.dayOfWeek}")
                    )
                }
                if (avail.slotMinutes !in 10..240) {
                    return Result.failure(
                        IllegalArgumentException("La duración del bloque debe ser entre 10 y 240 minutos para ${avail.dayOfWeek}")
                    )
                }
            }
        }

        return doctorRepository.saveDoctorAvailability(doctorId, availabilities)
    }
}
