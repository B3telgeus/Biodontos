package com.example.biodontos.domain.usecase.appointment

import com.example.biodontos.domain.repository.AppointmentRepository

class GetDoctorBusySlotsUseCase(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(doctorId: String, date: String): Result<List<String>> {
        return repository.getDoctorBusySlots(doctorId, date)
    }
}
