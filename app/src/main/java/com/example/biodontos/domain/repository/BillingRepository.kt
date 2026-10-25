package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.DoctorPaymentRecord
import com.example.biodontos.domain.model.PatientPaymentSummary
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus

interface BillingRepository {
    suspend fun getPaymentSummaryByPatientId(patientId: String): Result<PatientPaymentSummary>
    suspend fun getTreatmentsByPatientId(patientId: String): Result<List<Treatment>>
    suspend fun getPaymentsByPatientId(patientId: String): Result<List<Payment>>
    suspend fun createTreatment(treatment: Treatment): Result<Treatment>
    suspend fun recordPayment(payment: Payment): Result<Payment>
    suspend fun voidPayment(paymentId: String, reason: String): Result<Unit>
    suspend fun updateTreatmentStatus(
        treatmentId: String,
        newStatus: TreatmentStatus,
        reason: String? = null
    ): Result<Unit>
    suspend fun getRecentDoctorPayments(limit: Int = 10): Result<List<DoctorPaymentRecord>>
}
