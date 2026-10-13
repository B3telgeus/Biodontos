package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.DoctorDetail
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.ClinicClock

class GetDoctorDetailUseCase(
    private val doctorRepository: DoctorRepository,
    private val clinicClock: ClinicClock
) {
    suspend operator fun invoke(doctorId: String): Result<DoctorDetail> {
        val doctorResult = doctorRepository.getDoctorById(doctorId)
        val doctor = doctorResult.getOrNull()
            ?: return Result.failure(doctorResult.exceptionOrNull() ?: NoSuchElementException("Doctor no encontrado"))

        val availabilityResult = doctorRepository.getDoctorAvailability(doctorId)
        val availability = availabilityResult.getOrDefault(emptyList())

        val now = clinicClock.now()
        val today = now.dayOfWeek
        val currentTime = now.toLocalTime()

        val isAvailableToday = availability.any {
            it.dayOfWeek == today && it.isActive && currentTime.isBefore(it.endTime)
        }

        return Result.success(
            DoctorDetail(
                doctor = doctor,
                availability = availability,
                isAvailableToday = isAvailableToday
            )
        )
    }
}
