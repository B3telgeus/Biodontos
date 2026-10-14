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
    val id: String? = null,
    @SerialName("patient_id") val patientId: String,
    val description: String = "",
    @SerialName("total_cost") val totalCost: Double = 0.0,
    val status: String = "PENDIENTE",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PaymentDto(
    val id: String? = null,
    @SerialName("treatment_id") val treatmentId: String,
    val amount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String? = "EFECTIVO",
    @SerialName("created_at") val createdAt: String? = null
)

fun TreatmentDto.toDomain(): Treatment = Treatment(
    id = id ?: "",
    patientId = PatientId(patientId),
    description = description,
    totalCost = CopAmount(totalCost.toLong()),
    status = status,
    createdAt = createdAt?.let { parseIsoDate(it) }
)

fun Treatment.toDto(): TreatmentDto = TreatmentDto(
    id = id.trim().ifEmpty { null },
    patientId = patientId.value.trim(),
    description = description.trim(),
    totalCost = totalCost.pesos.toDouble(),
    status = if (status.isBlank() || status.equals("ACTIVE", ignoreCase = true)) "PENDIENTE" else status.trim().uppercase(),
    createdAt = createdAt?.let { java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) }
)

fun PaymentDto.toDomain(): Payment = Payment(
    id = id ?: "",
    treatmentId = treatmentId,
    amount = CopAmount(amount.toLong()),
    paymentMethod = paymentMethod ?: "EFECTIVO",
    createdAt = createdAt?.let { parseIsoDate(it) }
)

fun Payment.toDto(): PaymentDto = PaymentDto(
    id = id.trim().ifEmpty { null },
    treatmentId = treatmentId.trim(),
    amount = amount.pesos.toDouble(),
    paymentMethod = paymentMethod?.trim()?.ifEmpty { "EFECTIVO" } ?: "EFECTIVO",
    createdAt = createdAt?.let { java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) }
)

private fun parseIsoDate(str: String): LocalDateTime {
    return runCatching {
        OffsetDateTime.parse(str).toLocalDateTime()
    }.recoverCatching {
        LocalDateTime.parse(str)
    }.getOrDefault(LocalDateTime.now())
}
