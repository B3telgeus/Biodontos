package com.example.biodontos.domain.usecase.patients

import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.PatientRepository

class GetPatientsUseCase(private val repository: PatientRepository) {
    suspend operator fun invoke(): Result<List<Patient>> {
        return repository.getPatients()
    }
}
