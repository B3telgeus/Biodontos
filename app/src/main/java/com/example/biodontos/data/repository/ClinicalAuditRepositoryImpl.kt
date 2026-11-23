package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.ClinicalAuditLogDto
import com.example.biodontos.data.dto.ClinicalAuditLogInsertDto
import com.example.biodontos.domain.model.history.AuditAction
import com.example.biodontos.domain.model.history.AuditEntry
import com.example.biodontos.domain.repository.ClinicalAuditRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException

class ClinicalAuditRepositoryImpl : ClinicalAuditRepository {

    override suspend fun getAuditTrail(patientId: String, limit: Int): Result<List<AuditEntry>> {
        return try {
            val entries = SupabaseClient.client.from("clinical_audit_log")
                .select {
                    filter { eq("patient_id", patientId) }
                    order("occurred_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ClinicalAuditLogDto>()
                .map { it.toDomain() }
            Result.success(entries)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun recordAudit(
        patientId: String,
        entityTable: String,
        entityId: String,
        action: AuditAction,
        changedFields: List<String>,
        actorUserId: String?,
        actorName: String?
    ): Result<Unit> {
        return try {
            val insertDto = ClinicalAuditLogInsertDto(
                patientId = patientId,
                entityTable = entityTable,
                entityId = entityId,
                action = action.name,
                changedFields = changedFields,
                actorUserId = actorUserId,
                actorName = actorName
            )
            SupabaseClient.client.from("clinical_audit_log").insert(insertDto)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
