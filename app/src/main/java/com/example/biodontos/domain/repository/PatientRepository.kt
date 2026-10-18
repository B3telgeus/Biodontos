package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.Patient

interface PatientRepository {
    suspend fun getPatients(): Result<List<Patient>>
    suspend fun getPatientById(id: String): Result<Patient?>
    suspend fun getPatientByUserId(userId: String): Result<Patient?>
    suspend fun savePatient(patient: Patient): Result<Unit>
    suspend fun createOrUpdatePatient(patient: Patient): Result<Patient>
}
