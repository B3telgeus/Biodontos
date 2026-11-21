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
    @SerialName("patient_city") val patientCity: String? = null,
    @SerialName("patient_signature_url") val patientSignatureUrl: String? = null,
    @SerialName("guardian_signature_url") val guardianSignatureUrl: String? = null,
    @SerialName("doctor_signature_url") val doctorSignatureUrl: String? = null,
    @SerialName("doctor_license_number") val doctorLicenseNumber: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
    val status: String = "SIGNED",
    val revoked: Boolean = false,
    @SerialName("revoked_at") val revokedAt: String? = null,
    @SerialName("revocation_reason") val revocationReason: String? = null,
    @SerialName("consent_type") val consentType: String? = null,
    @SerialName("selected_procedures") val selectedProcedures: List<String> = emptyList(),
    @SerialName("acknowledged_risks") val acknowledgedRisks: List<String> = emptyList(),
    @SerialName("relevant_medical_conditions") val relevantMedicalConditions: String? = null,
    @SerialName("occlusion_test_accepted") val occlusionTestAccepted: Boolean = false,
    @SerialName("aesthetic_color_accepted") val aestheticColorAccepted: Boolean = false,
    @SerialName("initial_color_guide") val initialColorGuide: String? = null,
    @SerialName("final_color_guide") val finalColorGuide: String? = null,
    @SerialName("specific_notes") val specificNotes: String? = null
)

@Serializable
data class InformedConsentProcedureDto(
    val id: String? = null,
    @SerialName("consent_id") val consentId: String,
    @SerialName("tooth_number") val toothNumber: Int,
    @SerialName("procedure_date") val procedureDate: String,
    @SerialName("patient_signature_url") val patientSignatureUrl: String? = null,
    @SerialName("procedure_name") val procedureName: String? = null,
    @SerialName("doctor_signature_url") val doctorSignatureUrl: String? = null
)
