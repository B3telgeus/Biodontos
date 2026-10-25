package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.history.AuditEntry

interface ClinicalAuditRepository {
    suspend fun getAuditTrail(patientId: String, limit: Int = 500): Result<List<AuditEntry>>
}
