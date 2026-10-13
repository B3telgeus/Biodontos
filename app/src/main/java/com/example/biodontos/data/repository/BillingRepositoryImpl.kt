package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.PaymentDto
import com.example.biodontos.data.dto.TreatmentDto
import com.example.biodontos.data.dto.toDomain
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.PatientPaymentSummary
import com.example.biodontos.domain.model.TreatmentBalance
import com.example.biodontos.domain.repository.BillingRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class BillingRepositoryImpl : BillingRepository {

    override suspend fun getPaymentSummaryByPatientId(patientId: String): Result<PatientPaymentSummary> {
        return try {
            // 1. Obtener tratamientos del paciente
            val treatments = SupabaseClient.client.from("treatments")
                .select {
                    filter { eq("patient_id", patientId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<TreatmentDto>()
                .map { it.toDomain() }

            if (treatments.isEmpty()) {
                return Result.success(
                    PatientPaymentSummary(
                        totalPending = CopAmount(0),
                        totalPaid = CopAmount(0),
                        balances = emptyList(),
                        recentPayments = emptyList()
                    )
                )
            }

            val treatmentIds = treatments.map { it.id }

            // 2. Obtener pagos de los tratamientos
            val payments = SupabaseClient.client.from("payments")
                .select {
                    filter { isIn("treatment_id", treatmentIds) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<PaymentDto>()
                .map { it.toDomain() }

            // 3. Agrupar pagos por tratamiento y calcular balances
            val paymentsByTreatment = payments.groupBy { it.treatmentId }

            val balances = treatments.map { treatment ->
                val trPayments = paymentsByTreatment[treatment.id] ?: emptyList()
                val paidSum = trPayments.fold(CopAmount(0)) { acc, p -> acc + p.amount }
                TreatmentBalance(
                    treatment = treatment,
                    paidAmount = paidSum,
                    payments = trPayments
                )
            }

            val totalPending = balances.fold(CopAmount(0)) { acc, b -> acc + b.pendingAmount }
            val totalPaid = balances.fold(CopAmount(0)) { acc, b -> acc + b.paidAmount }

            Result.success(
                PatientPaymentSummary(
                    totalPending = totalPending,
                    totalPaid = totalPaid,
                    balances = balances,
                    recentPayments = payments.take(5)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
