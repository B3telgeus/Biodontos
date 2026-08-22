package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Patient

interface PatientRepository {
    suspend fun getPatients(): Result<List<Patient>>
    suspend fun savePatient(patient: Patient): Result<Unit>
}
