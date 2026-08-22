package com.example.biodontos.domain.usecase.patients

import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.PatientRepository

class SavePatientUseCase(private val repository: PatientRepository) {
    suspend operator fun invoke(patient: Patient): Result<Unit> {
        return repository.savePatient(patient)
    }
}
