package com.example.biodontos.data.repository

import com.example.biodontos.data.dto.EvolutionDto
import com.example.biodontos.data.dto.EvolutionMaterialUsedDto
import com.example.biodontos.data.dto.SaveEvolutionRpcParams
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.EvolutionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.postgrest.rpc as supabaseRpc
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.biodontos.core.network.SupabaseClient as AppSupabaseClient

class EvolutionRepositoryImpl(
    private val supabaseClient: SupabaseClient = AppSupabaseClient.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    internal var postgrestUpdater: (suspend (table: String, update: PostgrestUpdate.() -> Unit, request: PostgrestRequestBuilder.() -> Unit) -> Unit) = { table, update, request ->
        supabaseClient.postgrest.from(table).update(update, request)
    }
) : EvolutionRepository {

    override suspend fun getEvolutionsByRecordId(recordId: String): Result<List<Evolution>> {
        return withContext(ioDispatcher) {
            try {
                val evolutionsDto = supabaseClient.postgrest.from("evolutions")
                    .select {
                        filter { eq("record_id", recordId) }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<EvolutionDto>()

                val evoIds = evolutionsDto.mapNotNull { it.id }.filter { it.isNotBlank() }
                val materialsByEvoId = if (evoIds.isNotEmpty()) {
                    supabaseClient.postgrest.from("evolution_materials_used")
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
    }

    override suspend fun saveEvolution(
        evolution: Evolution,
        closeRecord: Boolean,
        closureSummary: String?,
        closureSignatureUrl: String?
    ): Result<Evolution> {
        return withContext(ioDispatcher) {
            try {
                val resolvedEvoId = evolution.id.trim().ifEmpty { UUID.randomUUID().toString() }
                val resolvedEvolution = if (evolution.id.isBlank()) evolution.copy(id = resolvedEvoId) else evolution

                val dto = resolvedEvolution.toDto()
                val materialDtos = resolvedEvolution.materialsUsed.map { it.toDto(resolvedEvoId) }

                val rpcParams = SaveEvolutionRpcParams(
                    evolution = dto,
                    materials = materialDtos,
                    closeRecord = closeRecord,
                    closureSummary = closureSummary,
                    closureSignatureUrl = closureSignatureUrl
                )

                supabaseClient.postgrest.rpc(
                    function = "save_evolution_with_materials",
                    parameters = rpcParams
                )

                Result.success(resolvedEvolution)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    override suspend fun voidEvolution(evolutionId: String, reason: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                val trimmedReason = reason.trim()
                val nowIso = Instant.now().toString()

                val currentUserId = try {
                    supabaseClient.auth.currentUserOrNull()?.id
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }

                postgrestUpdater(
                    "evolutions",
                    {
                        set("voided_at", nowIso)
                        set("void_reason", trimmedReason)
                        if (currentUserId != null) {
                            set("voided_by", currentUserId)
                        } else {
                            set("voided_by", null as String?)
                        }
                    },
                    {
                        filter { eq("id", evolutionId) }
                    }
                )
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
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

suspend fun Postgrest.rpc(
    function: String,
    parameters: Any? = null
): PostgrestResult {
    return this.supabaseRpc(function = function, parameters = parameters ?: Unit)
}
