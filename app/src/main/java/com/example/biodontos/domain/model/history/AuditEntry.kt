package com.example.biodontos.domain.model.history

import java.time.LocalDateTime

enum class AuditAction(val label: String) {
    CREATE("Creado"),
    UPDATE("Actualizado"),
    VOID("Anulado"),
    STATUS_CHANGE("Cambio de estado");

    companion object {
        fun fromString(raw: String): AuditAction =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: UPDATE
    }
}

data class AuditStamp(
    val actorName: String?,
    val action: AuditAction,
    val occurredAt: LocalDateTime
)

data class VoidInfo(
    val voidedAt: LocalDateTime,
    val reason: String,
    val voidedBy: String? = null
)

data class AuditEntry(
    val id: String,
    val patientId: String,
    val entityTable: String,
    val entityId: String,
    val action: AuditAction,
    val changedFields: List<String> = emptyList(),
    val actorUserId: String? = null,
    val actorName: String? = null,
    val occurredAt: LocalDateTime
) {
    fun toStamp(): AuditStamp = AuditStamp(
        actorName = actorName,
        action = action,
        occurredAt = occurredAt
    )
}
