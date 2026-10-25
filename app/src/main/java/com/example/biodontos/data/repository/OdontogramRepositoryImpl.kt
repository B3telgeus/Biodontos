package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.ClinicalRecordDto
import com.example.biodontos.data.dto.EvolutionDto
import com.example.biodontos.data.dto.ToothStateDto
import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface
import com.example.biodontos.domain.repository.OdontogramRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OdontogramRepositoryImpl : OdontogramRepository {

    override suspend fun getOdontogram(patientId: String): Result<List<Tooth>> = withContext(Dispatchers.IO) {
        try {
            // 1. Get Clinical Record
            val record = SupabaseClient.client.from("clinical_records")
                .select { filter { ClinicalRecordDto::patientId eq patientId } }
                .decodeSingleOrNull<ClinicalRecordDto>() ?: return@withContext Result.success(emptyList())

            // 2. Get all tooth states for this record via evolutions
            // For simplicity, we'll fetch all and group by tooth/surface in memory
            // A more efficient way would be a RPC or a complex join
            val recordId = record.id ?: return@withContext Result.success(emptyList())
            val evolutions = SupabaseClient.client.from("evolutions")
                .select { filter { EvolutionDto::recordId eq recordId } }
                .decodeList<EvolutionDto>()
            
            if (evolutions.isEmpty()) return@withContext Result.success(emptyList())
            
            val evolutionIds = evolutions.mapNotNull { it.id }
            val toothStates = SupabaseClient.client.from("tooth_states")
                .select { filter { ToothStateDto::evolutionId isIn evolutionIds } }
                .decodeList<ToothStateDto>()

            // Group by tooth number and surface, taking the latest one (assuming created_at order or last in list)
            val toothMap = mutableMapOf<Int, MutableMap<ToothSurface, ToothCondition>>()
            
            toothStates.forEach { dto ->
                val surface = try { ToothSurface.valueOf(dto.surface) } catch (e: Exception) { null }
                val condition = try { ToothCondition.valueOf(dto.condition) } catch (e: Exception) { null }
                
                if (surface != null && condition != null) {
                    val surfaces = toothMap.getOrPut(dto.toothNumber) { mutableMapOf() }
                    surfaces[surface] = condition
                }
            }

            val teeth = toothMap.map { (number, surfaces) ->
                Tooth(number, surfaces)
            }
            
            Result.success(teeth)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateToothState(
        patientId: String,
        toothNumber: Int,
        surface: ToothSurface,
        condition: ToothCondition,
        comment: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Get or create Clinical Record
            var record = SupabaseClient.client.from("clinical_records")
                .select { filter { ClinicalRecordDto::patientId eq patientId } }
                .decodeSingleOrNull<ClinicalRecordDto>()
            
            if (record == null) {
                record = SupabaseClient.client.from("clinical_records")
                    .insert(ClinicalRecordDto(patientId = patientId)) { select() }
                    .decodeSingle<ClinicalRecordDto>()
            }

            val validRecordId = requireNotNull(record.id) { "Clinical record ID no puede ser nulo al registrar evolución" }

            // 2. Create Evolution
            val evolution = SupabaseClient.client.from("evolutions")
                .insert(EvolutionDto(
                    recordId = validRecordId,
                    description = comment ?: "Actualización de diente $toothNumber",
                    doctorName = "Doctor General" // Placeholder
                )) { select() }
                .decodeSingle<EvolutionDto>()

            val validEvolutionId = requireNotNull(evolution.id) { "Evolution ID no puede ser nulo al registrar superficie" }

            // 3. Insert Tooth State
            SupabaseClient.client.from("tooth_states").insert(
                ToothStateDto(
                    evolutionId = validEvolutionId,
                    toothNumber = toothNumber,
                    surface = surface.name,
                    condition = condition.name
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
