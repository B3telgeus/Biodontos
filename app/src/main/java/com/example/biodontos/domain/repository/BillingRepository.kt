package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.PatientPaymentSummary

interface BillingRepository {
    suspend fun getPaymentSummaryByPatientId(patientId: String): Result<PatientPaymentSummary>
}
