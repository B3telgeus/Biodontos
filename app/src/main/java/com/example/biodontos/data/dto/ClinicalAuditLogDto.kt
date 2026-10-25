package com.example.biodontos.data.dto

import com.example.biodontos.domain.model.history.AuditAction
import com.example.biodontos.domain.model.history.AuditEntry
import com.example.biodontos.domain.model.history.ClinicalDateParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClinicalAuditLogDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("entity_table") val entityTable: String,
    @SerialName("entity_id") val entityId: String,
    val action: String,
    @SerialName("changed_fields") val changedFields: List<String> = emptyList(),
    @SerialName("actor_user_id") val actorUserId: String? = null,
    @SerialName("actor_name") val actorName: String? = null,
    @SerialName("occurred_at") val occurredAt: String
) {
    fun toDomain(): AuditEntry = AuditEntry(
        id = id,
        patientId = patientId,
        entityTable = entityTable,
        entityId = entityId,
        action = AuditAction.fromString(action),
        changedFields = changedFields,
        actorUserId = actorUserId,
        actorName = actorName,
        occurredAt = ClinicalDateParser.parseIsoDateTime(occurredAt).dateTime
    )
}
