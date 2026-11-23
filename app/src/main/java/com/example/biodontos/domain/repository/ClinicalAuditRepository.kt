package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.history.AuditAction
import com.example.biodontos.domain.model.history.AuditEntry

interface ClinicalAuditRepository {
    suspend fun getAuditTrail(patientId: String, limit: Int = 500): Result<List<AuditEntry>>
    suspend fun recordAudit(
        patientId: String,
        entityTable: String,
        entityId: String,
        action: AuditAction,
        changedFields: List<String> = emptyList(),
        actorUserId: String? = null,
        actorName: String? = null
    ): Result<Unit>
}
