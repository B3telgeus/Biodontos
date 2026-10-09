package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.ClinicalRecordDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.ClinicalRecordStatus
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import io.github.jan.supabase.postgrest.from
import java.time.Instant

class ClinicalRecordRepositoryImpl : ClinicalRecordRepository {

    override suspend fun getRecordByPatientId(patientId: String): Result<ClinicalRecord?> {
        return try {
            val result = SupabaseClient.client.from("clinical_records")
                .select { filter { eq("patient_id", patientId) } }
                .decodeSingleOrNull<ClinicalRecordDto>()
            Result.success(result?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveRecord(record: ClinicalRecord): Result<ClinicalRecord> {
        return try {
            val dto = record.toDto()
            val saved = if (record.id.isBlank()) {
                SupabaseClient.client.from("clinical_records").upsert(dto) {
                    select()
                }.decodeSingle<ClinicalRecordDto>()
            } else {
                SupabaseClient.client.from("clinical_records").upsert(dto) {
                    select()
                }.decodeSingle<ClinicalRecordDto>()
            }
            Result.success(saved.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeRecord(
        recordId: String,
        doctorId: String,
        summary: String,
        signatureUrl: String?
    ): Result<Unit> {
        return try {
            val nowIso = Instant.now().toString()
            SupabaseClient.client.from("clinical_records").update(
                {
                    set("status", ClinicalRecordStatus.CERRADA.name)
                    set("closed_at", nowIso)
                    set("closed_by_doctor_id", doctorId)
                    set("closure_summary", summary)
                    set("closure_signature_url", signatureUrl)
                }
            ) {
                filter { eq("id", recordId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
