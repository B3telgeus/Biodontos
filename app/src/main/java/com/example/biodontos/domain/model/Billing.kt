package com.example.biodontos.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Treatment(
    val id: String,
    val patientId: PatientId,
    val description: String,
    val totalCost: CopAmount,
    val status: String = "ACTIVE",
    val createdAt: LocalDateTime? = null
)

data class Payment(
    val id: String,
    val treatmentId: String,
    val amount: CopAmount,
    val paymentMethod: String? = null,
    val createdAt: LocalDateTime? = null
)

data class TreatmentBalance(
    val treatment: Treatment,
    val paidAmount: CopAmount,
    val payments: List<Payment> = emptyList()
) {
    val pendingAmount: CopAmount
        get() = treatment.totalCost - paidAmount

    val isPaidInFull: Boolean
        get() = pendingAmount.pesos <= 0L

    val progressFraction: Float
        get() = if (treatment.totalCost.pesos == 0L) 1.0f 
                else (paidAmount.pesos.toFloat() / treatment.totalCost.pesos.toFloat()).coerceIn(0.0f, 1.0f)
}

data class PatientPaymentSummary(
    val totalPending: CopAmount,
    val totalPaid: CopAmount,
    val balances: List<TreatmentBalance>,
    val recentPayments: List<Payment>
)
