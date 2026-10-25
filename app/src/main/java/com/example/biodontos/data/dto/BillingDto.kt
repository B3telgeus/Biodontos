package com.example.biodontos.data.dto

import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.history.ClinicalDateParser
import com.example.biodontos.domain.model.history.VoidInfo
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TreatmentDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String,
    val description: String = "",
    @SerialName("total_cost") val totalCost: Double = 0.0,
    val status: String = "PENDIENTE",
    @SerialName("status_reason") val statusReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PaymentDto(
    val id: String? = null,
    @SerialName("treatment_id") val treatmentId: String,
    val amount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String? = "EFECTIVO",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("voided_at") val voidedAt: String? = null,
    @SerialName("voided_by") val voidedBy: String? = null,
    @SerialName("void_reason") val voidReason: String? = null
)

fun TreatmentDto.toDomain(): Treatment = Treatment(
    id = id ?: "",
    patientId = PatientId(patientId),
    description = description,
    totalCost = CopAmount(totalCost.toLong()),
    status = status,
    createdAt = createdAt?.let { parseIsoDate(it) },
    statusReason = statusReason
)

fun Treatment.toDto(): TreatmentDto = TreatmentDto(
    id = id.trim().ifEmpty { null },
    patientId = patientId.value.trim(),
    description = description.trim(),
    totalCost = totalCost.pesos.toDouble(),
    status = if (status.isBlank() || status.equals("ACTIVE", ignoreCase = true)) {
        "PENDIENTE"
    } else {
        status.trim().uppercase()
    },
    statusReason = statusReason,
    createdAt = createdAt?.let { java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) }
)

fun PaymentDto.toDomain(): Payment {
    val resolvedVoidInfo = if (!voidedAt.isNullOrBlank()) {
        VoidInfo(
            voidedAt = ClinicalDateParser.parseIsoDateTime(voidedAt).dateTime,
            reason = voidReason ?: "Anulado",
            voidedBy = voidedBy
        )
    } else {
        null
    }
    return Payment(
        id = id ?: "",
        treatmentId = treatmentId,
        amount = CopAmount(amount.toLong()),
        paymentMethod = paymentMethod ?: "EFECTIVO",
        createdAt = createdAt?.let { parseIsoDate(it) },
        voidInfo = resolvedVoidInfo
    )
}

fun Payment.toDto(): PaymentDto = PaymentDto(
    id = id.trim().ifEmpty { null },
    treatmentId = treatmentId.trim(),
    amount = amount.pesos.toDouble(),
    paymentMethod = paymentMethod?.trim()?.ifEmpty { "EFECTIVO" } ?: "EFECTIVO",
    createdAt = createdAt?.let { java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) },
    voidedAt = voidInfo?.voidedAt?.let { java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) },
    voidedBy = voidInfo?.voidedBy,
    voidReason = voidInfo?.reason
)

private fun parseIsoDate(str: String): LocalDateTime {
    return runCatching {
        OffsetDateTime.parse(str).toLocalDateTime()
    }.recoverCatching {
        LocalDateTime.parse(str)
    }.getOrDefault(LocalDateTime.now())
}
