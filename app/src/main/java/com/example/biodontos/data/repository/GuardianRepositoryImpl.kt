package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.GuardianDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Guardian
import com.example.biodontos.domain.repository.GuardianRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException

class GuardianRepositoryImpl : GuardianRepository {

    override suspend fun getGuardianById(id: String): Result<Guardian?> {
        return try {
            val result = SupabaseClient.client.from("guardians")
                .select { filter { eq("id", id) } }
                .decodeSingleOrNull<GuardianDto>()
            Result.success(result?.toDomain())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveGuardian(guardian: Guardian): Result<Guardian> {
        return try {
            val dto = guardian.toDto()
            val savedDto = SupabaseClient.client.from("guardians").upsert(dto) {
                select()
            }.decodeSingle<GuardianDto>()
            Result.success(savedDto.toDomain())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
