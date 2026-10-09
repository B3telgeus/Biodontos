package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.EvolutionDto
import com.example.biodontos.data.dto.EvolutionMaterialUsedDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.EvolutionRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class EvolutionRepositoryImpl : EvolutionRepository {

    override suspend fun getEvolutionsByRecordId(recordId: String): Result<List<Evolution>> {
        return try {
            val evolutionsDto = SupabaseClient.client.from("evolutions")
                .select {
                    filter { eq("record_id", recordId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<EvolutionDto>()

            val resultList = evolutionsDto.map { evoDto ->
                val evoId = evoDto.id
                val materials = if (evoId != null) {
                    SupabaseClient.client.from("evolution_materials_used")
                        .select { filter { eq("evolution_id", evoId) } }
                        .decodeList<EvolutionMaterialUsedDto>()
                        .map { it.toDomain() }
                } else {
                    emptyList()
                }
                evoDto.toDomain(materials)
            }
            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveEvolution(evolution: Evolution): Result<Evolution> {
        return try {
            val dto = evolution.toDto()
            val savedEvo = SupabaseClient.client.from("evolutions").upsert(dto) {
                select()
            }.decodeSingle<EvolutionDto>()

            val evoId = savedEvo.id ?: evolution.id

            if (evolution.materialsUsed.isNotEmpty() && evoId.isNotBlank()) {
                val materialDtos = evolution.materialsUsed.map { it.toDto(evoId) }
                SupabaseClient.client.from("evolution_materials_used").upsert(materialDtos)
            }

            Result.success(savedEvo.toDomain(evolution.materialsUsed))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
