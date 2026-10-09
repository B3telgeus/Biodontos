package com.example.biodontos.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PeriodontalExaminationDto(
    val id: String? = null,
    @SerialName("record_id") val recordId: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("examination_date") val examinationDate: String,
    @SerialName("general_observations") val generalObservations: String? = null
)

@Serializable
data class PeriodontalSiteDto(
    val id: String? = null,
    @SerialName("examination_id") val examinationId: String,
    @SerialName("tooth_number") val toothNumber: Int,
    @SerialName("site_location") val siteLocation: String,
    @SerialName("pocket_depth_mm") val pocketDepthMm: Float = 1.0f,
    @SerialName("gingival_recession_mm") val gingivalRecessionMm: Float = 0.0f,
    @SerialName("clinical_attachment_loss_mm") val clinicalAttachmentLossMm: Float? = null,
    @SerialName("bleeding_on_probing") val bleedingOnProbing: Boolean = false,
    val suppuration: Boolean = false,
    @SerialName("mobility_grade") val mobilityGrade: Int = 0,
    @SerialName("furcation_grade") val furcationGrade: Int = 0
)
