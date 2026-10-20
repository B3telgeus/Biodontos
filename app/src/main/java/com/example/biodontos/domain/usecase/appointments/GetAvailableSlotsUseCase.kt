package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.scheduling.SlotCalculator
import com.example.biodontos.domain.scheduling.TimeSlot
import java.time.LocalDate

class GetAvailableSlotsUseCase(
    private val doctorRepository: DoctorRepository,
    private val appointmentRepository: AppointmentRepository,
    private val clinicClock: ClinicClock
) {
    suspend operator fun invoke(
        doctorId: DoctorId,
        date: LocalDate
    ): Result<List<TimeSlot>> {
        val availabilityResult = doctorRepository.getDoctorAvailability(doctorId.value)
        val availabilities = availabilityResult.getOrNull()
            ?: return Result.failure(availabilityResult.exceptionOrNull() ?: Exception("No se pudo obtener la disponibilidad del doctor"))

        val from = date.atStartOfDay()
        val to = date.atTime(23, 59, 59)
        val busyResult = appointmentRepository.getDoctorBusySlots(doctorId.value, from, to)
        val busyIntervals = busyResult.getOrDefault(emptyList())

        val now = clinicClock.now()
        val slots = SlotCalculator.calculateAvailableSlots(
            targetDate = date,
            availabilities = availabilities,
            busyIntervals = busyIntervals,
            currentTime = now,
            minLeadTimeHours = 2
        )
        return Result.success(slots)
    }
}
