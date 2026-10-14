package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.repository.AuthRepository
import com.example.biodontos.domain.repository.DoctorRepository

class GetCurrentDoctorUseCase(
    private val authRepository: AuthRepository,
    private val doctorRepository: DoctorRepository
) {
    suspend operator fun invoke(): Result<Doctor?> {
        val session = authRepository.getCurrentSession()
            ?: return Result.failure(Exception("No hay una sesión activa"))

        if (session.role != UserRole.DOCTOR) {
            return Result.failure(Exception("El usuario actual no tiene rol de doctor"))
        }

        return doctorRepository.getDoctorByUserId(session.id)
    }
}
