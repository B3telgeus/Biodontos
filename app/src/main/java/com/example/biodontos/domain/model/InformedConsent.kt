package com.example.biodontos.domain.model

data class InformedConsentProcedure(
    val id: String = "",
    val consentId: String,
    val toothNumber: Int,
    val procedureDate: String,
    val patientSignatureUrl: String? = null
)

data class InformedConsent(
    val id: String = "",
    val patientId: String,
    val doctorId: String,
    val documentVersion: String = "2",
    val versionDate: String = "2019-06-01",
    val treatmentSummary: String,
    val representativeName: String? = null,
    val representativeId: String? = null,
    val patientSignatureUrl: String? = null,
    val guardianSignatureUrl: String? = null,
    val doctorSignatureUrl: String? = null,
    val doctorLicenseNumber: String? = null,
    val signedAt: String? = null,
    val status: String = "SIGNED",
    val procedures: List<InformedConsentProcedure> = emptyList()
)
