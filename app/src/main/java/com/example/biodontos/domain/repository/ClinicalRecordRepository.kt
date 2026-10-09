package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.ClinicalRecord

interface ClinicalRecordRepository {
    suspend fun getRecordByPatientId(patientId: String): Result<ClinicalRecord?>
    suspend fun saveRecord(record: ClinicalRecord): Result<ClinicalRecord>
    suspend fun closeRecord(
        recordId: String,
        doctorId: String,
        summary: String,
        signatureUrl: String?
    ): Result<Unit>
}
