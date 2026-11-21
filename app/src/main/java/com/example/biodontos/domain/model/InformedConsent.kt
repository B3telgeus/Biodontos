package com.example.biodontos.domain.model

enum class ConsentType(val label: String, val code: String) {
    GENERAL_ADMISSION("Admisión General", "GENERAL_ADMISSION"),
    BLEACHING("Aclaramiento Dental / Blanqueamiento", "BLEACHING"),
    GENERAL_ODONTOLOGY("Odontología General", "GENERAL_ODONTOLOGY"),
    ORAL_REHABILITATION("Rehabilitación Oral", "ORAL_REHABILITATION");

    companion object {
        fun fromCode(code: String?): ConsentType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: GENERAL_ADMISSION
        }
    }
}

data class InformedConsentProcedure(
    val id: String = "",
    val consentId: String = "",
    val toothNumber: Int,
    val procedureDate: String,
    val patientSignatureUrl: String? = null,
    val procedureName: String = "",
    val doctorSignatureUrl: String? = null
)

data class InformedConsent(
    val id: String = "",
    val patientId: String,
    val doctorId: String,
    val consentType: ConsentType = ConsentType.GENERAL_ADMISSION,
    val documentVersion: String = "2",
    val versionDate: String = "2019-06-01",
    val treatmentSummary: String,
    val representativeName: String? = null,
    val representativeId: String? = null,
    val patientCity: String? = null,
    val patientSignatureUrl: String? = null,
    val guardianSignatureUrl: String? = null,
    val doctorSignatureUrl: String? = null,
    val doctorLicenseNumber: String? = null,
    val signedAt: String? = null,
    val status: String = "SIGNED",
    val procedures: List<InformedConsentProcedure> = emptyList(),
    val selectedProcedures: List<String> = emptyList(),
    val acknowledgedRisks: List<String> = emptyList(),
    val relevantMedicalConditions: String = "",
    val occlusionTestAccepted: Boolean = false,
    val aestheticColorAccepted: Boolean = false,
    val initialColorGuide: String = "",
    val finalColorGuide: String = "",
    val specificNotes: String = ""
)
