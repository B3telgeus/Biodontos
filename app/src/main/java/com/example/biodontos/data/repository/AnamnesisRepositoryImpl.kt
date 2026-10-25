package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.AnamnesisDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.repository.AnamnesisRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException

class AnamnesisRepositoryImpl : AnamnesisRepository {

    override suspend fun getAnamnesisByPatientId(patientId: String): Result<Anamnesis?> {
        return try {
            val result = SupabaseClient.client.from("anamnesis")
                .select { filter { eq("patient_id", patientId) } }
                .decodeSingleOrNull<AnamnesisDto>()
            Result.success(result?.toDomain())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveAnamnesis(anamnesis: Anamnesis): Result<Anamnesis> {
        return try {
            val dto = anamnesis.toDto()
            val saved = SupabaseClient.client.from("anamnesis").upsert(dto) {
                select()
            }.decodeSingle<AnamnesisDto>()
            Result.success(saved.toDomain())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
