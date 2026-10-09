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
            Result.failure(e)
        }
    }
}
