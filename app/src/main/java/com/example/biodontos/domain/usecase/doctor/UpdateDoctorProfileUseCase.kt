package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.repository.DoctorRepository

class UpdateDoctorProfileUseCase(
    private val doctorRepository: DoctorRepository
) {
    suspend operator fun invoke(doctor: Doctor): Result<Doctor> {
        if (doctor.id.value.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID del doctor no puede estar vacío"))
        }
        if (doctor.firstName.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre es obligatorio"))
        }
        if (doctor.lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("El apellido es obligatorio"))
        }
        if (doctor.specialty.isBlank()) {
            return Result.failure(IllegalArgumentException("La especialidad es obligatoria"))
        }
        if (doctor.licenseNumber.isBlank()) {
            return Result.failure(IllegalArgumentException("La tarjeta o cédula profesional es obligatoria"))
        }
        if (doctor.email.isNotBlank() && (!doctor.email.contains("@") || !doctor.email.contains("."))) {
            return Result.failure(IllegalArgumentException("El formato del correo electrónico no es válido"))
        }
        return doctorRepository.updateDoctorProfile(doctor)
    }
}
