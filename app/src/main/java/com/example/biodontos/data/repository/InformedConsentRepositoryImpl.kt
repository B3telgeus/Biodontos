package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.InformedConsentDto
import com.example.biodontos.data.dto.InformedConsentProcedureDto
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.repository.InformedConsentRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException

class InformedConsentRepositoryImpl : InformedConsentRepository {

    override suspend fun getConsentByPatientId(patientId: String): Result<InformedConsent?> {
        return try {
            val consentDto = SupabaseClient.client.from("informed_consents")
                .select {
                    filter { eq("patient_id", patientId) }
                    order("created_at", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeList<InformedConsentDto>()
                .firstOrNull() ?: return Result.success(null)

            val consentId = consentDto.id
            val procedures = if (consentId != null) {
                SupabaseClient.client.from("informed_consent_procedures")
                    .select { filter { eq("consent_id", consentId) } }
                    .decodeList<InformedConsentProcedureDto>()
                    .map { it.toDomain() }
            } else {
                emptyList()
            }

            Result.success(consentDto.toDomain(procedures))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getConsentsByPatientId(patientId: String): Result<List<InformedConsent>> {
        return try {
            val consentDtos = SupabaseClient.client.from("informed_consents")
                .select {
                    filter { eq("patient_id", patientId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<InformedConsentDto>()

            val consentIds = consentDtos.mapNotNull { it.id }.filter { it.isNotBlank() }
            val proceduresByConsentId = if (consentIds.isNotEmpty()) {
                SupabaseClient.client.from("informed_consent_procedures")
                    .select { filter { isIn("consent_id", consentIds) } }
                    .decodeList<InformedConsentProcedureDto>()
                    .groupBy { it.consentId }
                    .mapValues { (_, list) -> list.map { it.toDomain() } }
            } else {
                emptyMap()
            }

            val consents = consentDtos.map { dto ->
                val procs = dto.id?.let { proceduresByConsentId[it] } ?: emptyList()
                dto.toDomain(procs)
            }
            Result.success(consents)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveConsent(consent: InformedConsent): Result<InformedConsent> {
        return try {
            val dto = consent.toDto()
            val savedConsent = SupabaseClient.client.from("informed_consents").upsert(dto) {
                select()
            }.decodeSingle<InformedConsentDto>()

            val consentId = savedConsent.id ?: consent.id

            if (consent.procedures.isNotEmpty() && consentId.isNotBlank()) {
                val procDtos = consent.procedures.map { it.toDto(consentId) }
                SupabaseClient.client.from("informed_consent_procedures").upsert(procDtos)
            }

            Result.success(savedConsent.toDomain(consent.procedures))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
