package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.PeriodontalExaminationDto
import com.example.biodontos.data.dto.PeriodontalSiteDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.PeriodontalExamination
import com.example.biodontos.domain.repository.PeriodontalRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException

class PeriodontalRepositoryImpl : PeriodontalRepository {

    override suspend fun getExaminationByRecordId(recordId: String): Result<PeriodontalExamination?> {
        return try {
            val examDto = SupabaseClient.client.from("periodontal_examinations")
                .select {
                    filter { eq("record_id", recordId) }
                    order("created_at", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeList<PeriodontalExaminationDto>()
                .firstOrNull() ?: return Result.success(null)

            val examId = examDto.id ?: return Result.success(examDto.toDomain(emptyList()))

            val sitesDto = SupabaseClient.client.from("periodontal_sites")
                .select {
                    filter { eq("examination_id", examId) }
                }
                .decodeList<PeriodontalSiteDto>()

            val sites = sitesDto.map { it.toDomain() }
            Result.success(examDto.toDomain(sites))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveExamination(examination: PeriodontalExamination): Result<PeriodontalExamination> {
        return try {
            val examDto = examination.toDto()
            val savedExam = SupabaseClient.client.from("periodontal_examinations").upsert(examDto) {
                select()
            }.decodeSingle<PeriodontalExaminationDto>()

            val examId = savedExam.id ?: examination.id

            if (examination.sites.isNotEmpty()) {
                val siteDtos = examination.sites.map { it.toDto(examId) }
                // Upsert sites matching unique constraint (examination_id, tooth_number, site_location)
                SupabaseClient.client.from("periodontal_sites").upsert(siteDtos)
            }

            Result.success(savedExam.toDomain(examination.sites))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
