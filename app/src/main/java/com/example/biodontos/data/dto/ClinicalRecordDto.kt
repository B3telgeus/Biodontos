package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClinicalRecordDto(
    val id: String? = null,
    @SerialName("patient_id") val patientId: String,
    @SerialName("dentition_type") val dentitionType: String = "ADULT",
    @SerialName("main_diagnosis") val mainDiagnosis: String? = null,
    @SerialName("related_diagnoses") val relatedDiagnoses: String? = null,
    @SerialName("complementary_exams") val complementaryExams: String? = null,
    val status: String = "ABIERTA",
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("closed_by_doctor_id") val closedByDoctorId: String? = null,
    @SerialName("closure_summary") val closureSummary: String? = null,
    @SerialName("closure_signature_url") val closureSignatureUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
