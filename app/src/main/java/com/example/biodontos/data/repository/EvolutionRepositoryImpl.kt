package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.EvolutionDto
import com.example.biodontos.data.dto.EvolutionMaterialUsedDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.EvolutionRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException

class EvolutionRepositoryImpl : EvolutionRepository {

    override suspend fun getEvolutionsByRecordId(recordId: String): Result<List<Evolution>> {
        return try {
            val evolutionsDto = SupabaseClient.client.from("evolutions")
                .select {
                    filter { eq("record_id", recordId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<EvolutionDto>()

            val evoIds = evolutionsDto.mapNotNull { it.id }.filter { it.isNotBlank() }
            val materialsByEvoId = if (evoIds.isNotEmpty()) {
                SupabaseClient.client.from("evolution_materials_used")
                    .select { filter { isIn("evolution_id", evoIds) } }
                    .decodeList<EvolutionMaterialUsedDto>()
                    .groupBy { it.evolutionId }
                    .mapValues { (_, list) -> list.map { it.toDomain() } }
            } else {
                emptyMap()
            }

            val resultList = evolutionsDto.map { evoDto ->
                val materials = evoDto.id?.let { materialsByEvoId[it] } ?: emptyList()
                evoDto.toDomain(materials)
            }
            Result.success(resultList)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
                SupabaseClient.client.from("evolution_materials_used").insert(materialDtos)
            }

            Result.success(savedEvo.toDomain(evolution.materialsUsed))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun voidEvolution(evolutionId: String, reason: String): Result<Unit> {
        return try {
            val nowIso = Instant.now().toString()
            val currentUserId = runCatching {
                SupabaseClient.client.auth.currentUserOrNull()?.id
            }.getOrNull()

            SupabaseClient.client.from("evolutions").update(
                {
                    set("voided_at", nowIso)
                    set("void_reason", reason.trim())
                    if (currentUserId != null) {
                        set("voided_by", currentUserId)
                    }
                }
            ) {
                filter { eq("id", evolutionId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveClarification(
        originalEvolution: Evolution,
        clarificationText: String,
        doctorId: String,
        doctorName: String?
    ): Result<Evolution> {
        val today = LocalDate.now().toString()
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val clarificationEvolution = Evolution(
            recordId = originalEvolution.recordId,
            doctorId = doctorId.ifBlank { originalEvolution.doctorId },
            doctorName = doctorName ?: originalEvolution.doctorName,
            date = today,
            procedureTime = nowTime,
            description = "NOTA ACLARATORIA: ${clarificationText.trim()}",
            toothTreated = originalEvolution.toothTreated,
            cost = 0.0,
            paymentAmount = 0.0,
            balance = 0.0,
            amendsEvolutionId = originalEvolution.id
        )
        return saveEvolution(clarificationEvolution)
    }
}
