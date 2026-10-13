package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.DoctorAvailabilityDto
import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.data.dto.toDomain
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.DoctorRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class DoctorRepositoryImpl : DoctorRepository {

    override suspend fun getDoctors(): Result<List<Doctor>> {
        return try {
            val list = SupabaseClient.client.from("doctors")
                .select {
                    order("first_name", order = Order.ASCENDING)
                }
                .decodeList<DoctorDto>()
            Result.success(list.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDoctorById(id: String): Result<Doctor?> {
        return try {
            val dto = SupabaseClient.client.from("doctors")
                .select { filter { eq("id", id) } }
                .decodeSingleOrNull<DoctorDto>()
            Result.success(dto?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDoctorAvailability(doctorId: String): Result<List<WeeklyAvailability>> {
        return try {
            val list = SupabaseClient.client.from("doctor_availability")
                .select {
                    filter {
                        eq("doctor_id", doctorId)
                        eq("is_active", true)
                    }
                    order("day_of_week", order = Order.ASCENDING)
                }
                .decodeList<DoctorAvailabilityDto>()
            Result.success(list.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
