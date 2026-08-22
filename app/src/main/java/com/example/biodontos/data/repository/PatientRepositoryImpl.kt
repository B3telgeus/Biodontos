package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.PatientRepository
import io.github.jan.supabase.postgrest.from

class PatientRepositoryImpl : PatientRepository {
    override suspend fun getPatients(): Result<List<Patient>> {
        return try {
            val result = SupabaseClient.client.from("patients").select().decodeList<PatientDto>()
            Result.success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun savePatient(patient: Patient): Result<Unit> {
        return try {
            SupabaseClient.client.from("patients").insert(patient.toDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
