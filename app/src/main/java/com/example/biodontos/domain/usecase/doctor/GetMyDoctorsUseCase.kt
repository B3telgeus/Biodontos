package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.DoctorRepository

class GetMyDoctorsUseCase(
    private val doctorRepository: DoctorRepository,
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(patientId: String): Result<List<Doctor>> {
        val allDoctorsResult = doctorRepository.getDoctors()
        val allDoctors = allDoctorsResult.getOrNull() ?: return allDoctorsResult

        val appointmentsResult = appointmentRepository.getAppointmentsByPatientId(patientId)
        val appointments = appointmentsResult.getOrDefault(emptyList())

        val patientDoctorIds = appointments
            .mapNotNull { it.doctorId }
            .filter { it.isNotBlank() }
            .toSet()

        if (patientDoctorIds.isEmpty()) {
            return Result.success(allDoctors)
        }

        val myDoctors = allDoctors.filter { it.id.value in patientDoctorIds }
        return Result.success(if (myDoctors.isNotEmpty()) myDoctors else allDoctors)
    }
}
