package com.example.biodontos.domain.usecase.patient

import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.repository.AuthRepository
import com.example.biodontos.domain.repository.PatientRepository

class GetCurrentPatientUseCase(
    private val authRepository: AuthRepository,
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(): Result<Patient?> {
        val session = authRepository.getCurrentSession() 
            ?: return Result.failure(Exception("No hay una sesión activa"))

        if (session.role != UserRole.PATIENT) {
            return Result.failure(Exception("El usuario actual no tiene rol de paciente"))
        }

        return patientRepository.getPatientByUserId(session.id)
    }
}
