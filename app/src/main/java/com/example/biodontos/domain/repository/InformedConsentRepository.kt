package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.InformedConsent

interface InformedConsentRepository {
    suspend fun getConsentByPatientId(patientId: String): Result<InformedConsent?>
    suspend fun getConsentsByPatientId(patientId: String): Result<List<InformedConsent>>
    suspend fun saveConsent(consent: InformedConsent): Result<InformedConsent>
}
