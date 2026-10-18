package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.repository.DoctorRepository
import io.github.jan.supabase.postgrest.from

class DoctorRepositoryImpl : DoctorRepository {

    override suspend fun getDoctors(): Result<List<Doctor>> {
        return try {
            val list = SupabaseClient.client.from("doctors")
                .select()
                .decodeList<DoctorDto>()
            Result.success(list.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDoctorById(id: String): Result<Doctor?> {
        return try {
            val dto = SupabaseClient.client.from("doctors")
                .select {
                    filter {
                        or {
                            eq("id", id)
                            eq("user_id", id)
                        }
                    }
                }
                .decodeSingleOrNull<DoctorDto>()
            Result.success(dto?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDoctorByUserId(userId: String): Result<Doctor?> {
        return getDoctorById(userId)
    }
}
