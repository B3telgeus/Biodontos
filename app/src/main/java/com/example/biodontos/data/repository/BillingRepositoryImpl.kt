package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.PaymentDto
import com.example.biodontos.data.dto.TreatmentDto
import com.example.biodontos.data.dto.toDomain
import com.example.biodontos.data.dto.toDto
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.DoctorPaymentRecord
import com.example.biodontos.domain.model.PatientPaymentSummary
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
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

    override suspend fun getTreatmentsByPatientId(patientId: String): Result<List<Treatment>> {
        return try {
            val list = SupabaseClient.client.from("treatments")
                .select {
                    filter { eq("patient_id", patientId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<TreatmentDto>()
                .map { it.toDomain() }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTreatment(treatment: Treatment): Result<Treatment> {
        return try {
            val dto = treatment.toDto()
            val savedDto = SupabaseClient.client.from("treatments")
                .insert(dto) { select() }
                .decodeSingle<TreatmentDto>()
            Result.success(savedDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordPayment(payment: Payment): Result<Payment> {
        return try {
            val dto = payment.toDto()
            val savedDto = SupabaseClient.client.from("payments")
                .insert(dto) { select() }
                .decodeSingle<PaymentDto>()
            Result.success(savedDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentDoctorPayments(limit: Int): Result<List<DoctorPaymentRecord>> {
        return try {
            val payments = SupabaseClient.client.from("payments")
                .select {
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<PaymentDto>()

            if (payments.isEmpty()) {
                return Result.success(emptyList())
            }

            val treatmentIds = payments.map { it.treatmentId }.distinct()
            val treatments = SupabaseClient.client.from("treatments")
                .select {
                    filter { isIn("id", treatmentIds) }
                }
                .decodeList<TreatmentDto>()
                .associateBy { it.id ?: "" }

            val patientIds = treatments.values.map { it.patientId }.distinct()
            val patients = if (patientIds.isNotEmpty()) {
                SupabaseClient.client.from("patients")
                    .select {
                        filter { isIn("id", patientIds) }
                    }
                    .decodeList<com.example.biodontos.data.dto.PatientDto>()
                    .associateBy { it.id ?: "" }
            } else {
                emptyMap()
            }

            val records = payments.map { p ->
                val tr = treatments[p.treatmentId]
                val pat = tr?.let { patients[it.patientId] }
                val patName = pat?.let {
                    val last = it.lastName.ifBlank { it.lastNameCol ?: "" }
                    "${it.firstName} $last".trim().ifBlank { "Paciente" }
                } ?: "Paciente"

                DoctorPaymentRecord(
                    id = p.id ?: "",
                    patientId = tr?.patientId ?: "",
                    patientName = patName,
                    treatmentId = p.treatmentId,
                    treatmentDescription = tr?.description?.ifBlank { "Tratamiento Odontológico" } ?: "Tratamiento Odontológico",
                    amount = CopAmount(p.amount.toLong()),
                    paymentMethod = p.paymentMethod ?: "EFECTIVO",
                    createdAt = p.toDomain().createdAt ?: java.time.LocalDateTime.now()
                )
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
