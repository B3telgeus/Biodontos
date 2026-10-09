package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.ClinicalMaterialDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.repository.MaterialRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class MaterialRepositoryImpl : MaterialRepository {

    override suspend fun getMaterials(): Result<List<ClinicalMaterial>> {
        return try {
            val list = SupabaseClient.client.from("clinical_materials")
                .select {
                    order("name", order = Order.ASCENDING)
                }
                .decodeList<ClinicalMaterialDto>()
            Result.success(list.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveMaterial(material: ClinicalMaterial): Result<ClinicalMaterial> {
        return try {
            val dto = material.toDto()
            val saved = SupabaseClient.client.from("clinical_materials").upsert(dto) {
                select()
            }.decodeSingle<ClinicalMaterialDto>()
            Result.success(saved.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
