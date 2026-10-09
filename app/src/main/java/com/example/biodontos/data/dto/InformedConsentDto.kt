package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InformedConsentDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("document_version") val documentVersion: String = "2",
    @SerialName("version_date") val versionDate: String = "2019-06-01",
    @SerialName("treatment_summary") val treatmentSummary: String,
    @SerialName("representative_name") val representativeName: String? = null,
    @SerialName("representative_id") val representativeId: String? = null,
    @SerialName("patient_signature_url") val patientSignatureUrl: String? = null,
    @SerialName("guardian_signature_url") val guardianSignatureUrl: String? = null,
    @SerialName("doctor_signature_url") val doctorSignatureUrl: String? = null,
    @SerialName("doctor_license_number") val doctorLicenseNumber: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
    val status: String = "SIGNED",
    val revoked: Boolean = false,
    @SerialName("revoked_at") val revokedAt: String? = null,
    @SerialName("revocation_reason") val revocationReason: String? = null
)

@Serializable
data class InformedConsentProcedureDto(
    val id: String? = null,
    @SerialName("consent_id") val consentId: String,
    @SerialName("tooth_number") val toothNumber: Int,
    @SerialName("procedure_date") val procedureDate: String,
    @SerialName("patient_signature_url") val patientSignatureUrl: String? = null
)
