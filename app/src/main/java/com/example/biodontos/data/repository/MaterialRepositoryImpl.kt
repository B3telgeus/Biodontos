package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.ClinicalMaterialDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.repository.MaterialRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import kotlinx.coroutines.CancellationException

class MaterialRepositoryImpl : MaterialRepository {

    override suspend fun getMaterials(
        includeInactive: Boolean,
        doctorId: DoctorId?
    ): Result<List<ClinicalMaterial>> {
        return try {
            val list = SupabaseClient.client.from("clinical_materials")
                .select {
                    filter {
                        if (!includeInactive) {
                            eq("is_active", true)
                        }
                        if (doctorId != null) {
                            eq("doctor_id", doctorId.value)
                        }
                    }
                    order("name", order = Order.ASCENDING)
                }
                .decodeList<ClinicalMaterialDto>()
            Result.success(list.map { it.toDomain() })
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getMaterialById(id: MaterialId): Result<ClinicalMaterial?> {
        return try {
            val dto = SupabaseClient.client.from("clinical_materials")
                .select {
                    filter {
                        eq("id", id.value)
                    }
                }
                .decodeSingleOrNull<ClinicalMaterialDto>()
            Result.success(dto?.toDomain())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveMaterial(material: ClinicalMaterial): Result<ClinicalMaterial> {
        return try {
            val dto = material.toDto()
            val saved = SupabaseClient.client.from("clinical_materials")
                .upsert(dto) {
                    select()
                }
                .decodeSingle<ClinicalMaterialDto>()
            Result.success(saved.toDomain())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun setMaterialActive(id: MaterialId, isActive: Boolean): Result<Unit> {
        return try {
            val nowIso = Instant.now().toString()
            SupabaseClient.client.from("clinical_materials")
                .update(
                    {
                        set("is_active", isActive)
                        set("updated_at", nowIso)
                    }
                ) {
                    filter {
                        eq("id", id.value)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
