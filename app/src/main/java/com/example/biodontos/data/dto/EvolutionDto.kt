package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvolutionDto(
    val id: String? = null,
    @SerialName("record_id") val recordId: String,
    @SerialName("doctor_id") val doctorId: String? = null,
    val date: String? = null,
    @SerialName("procedure_time") val procedureTime: String? = null,
    val description: String,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("tooth_treated") val toothTreated: Int? = null,
    val cost: Double = 0.0,
    @SerialName("payment_amount") val paymentAmount: Double = 0.0,
    val balance: Double = 0.0,
    @SerialName("patient_signature_url") val patientSignatureUrl: String? = null,
    @SerialName("doctor_signature_url") val doctorSignatureUrl: String? = null
)

@Serializable
data class EvolutionMaterialUsedDto(
    val id: String? = null,
    @SerialName("evolution_id") val evolutionId: String,
    @SerialName("material_id") val materialId: String,
    @SerialName("quantity_used") val quantityUsed: Double = 1.0,
    @SerialName("lot_number_snapshot") val lotNumberSnapshot: String,
    @SerialName("invima_snapshot") val invimaSnapshot: String,
    @SerialName("expiration_date_snapshot") val expirationDateSnapshot: String
)
