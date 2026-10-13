package com.example.biodontos.data.dto

import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.PatientId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Serializable
data class TreatmentDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    val description: String = "",
    @SerialName("total_cost") val totalCost: Double = 0.0,
    val status: String = "ACTIVE",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PaymentDto(
    val id: String,
    @SerialName("treatment_id") val treatmentId: String,
    val amount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

fun TreatmentDto.toDomain(): Treatment = Treatment(
    id = id,
    patientId = PatientId(patientId),
    description = description,
    totalCost = CopAmount(totalCost.toLong()),
    status = status,
    createdAt = createdAt?.let { parseIsoDate(it) }
)

fun PaymentDto.toDomain(): Payment = Payment(
    id = id,
    treatmentId = treatmentId,
    amount = CopAmount(amount.toLong()),
    paymentMethod = paymentMethod,
    createdAt = createdAt?.let { parseIsoDate(it) }
)

private fun parseIsoDate(str: String): LocalDateTime {
    return runCatching {
        OffsetDateTime.parse(str).toLocalDateTime()
    }.recoverCatching {
        LocalDateTime.parse(str)
    }.getOrDefault(LocalDateTime.now())
}
