package com.example.biodontos.domain.usecase.clinical

import com.example.biodontos.domain.model.ConsentType
import com.example.biodontos.domain.model.InformedConsent

sealed interface ConsentRequirementStatus {
    object NotRequired : ConsentRequirementStatus

    data class Satisfied(
        val consent: InformedConsent,
        val type: ConsentType
    ) : ConsentRequirementStatus

    data class Missing(
        val requiredType: ConsentType,
        val reason: String
    ) : ConsentRequirementStatus
}

class ValidateEvolutionConsentRequirementUseCase {

    operator fun invoke(
        description: String,
        toothTreated: Int?,
        consents: List<InformedConsent>
    ): ConsentRequirementStatus {
        val descLower = description.lowercase().trim()

        val requiredType = determineRequiredConsentType(descLower, toothTreated)
            ?: return ConsentRequirementStatus.NotRequired

        val existingConsent = consents.firstOrNull { consent ->
            consent.consentType == requiredType &&
                consent.status == "SIGNED" &&
                (!consent.patientSignatureUrl.isNullOrBlank() || !consent.guardianSignatureUrl.isNullOrBlank())
        }

        return if (existingConsent != null) {
            ConsentRequirementStatus.Satisfied(existingConsent, requiredType)
        } else {
            val reason = when (requiredType) {
                ConsentType.BLEACHING ->
                    "El procedimiento describe aclaramiento/blanqueamiento dental y requiere consentimiento específico firmado."
                ConsentType.ORAL_REHABILITATION ->
                    "El procedimiento involucra rehabilitación protésica/restauración compleja y requiere consentimiento de rehabilitación oral con prueba oclusal."
                ConsentType.GENERAL_ODONTOLOGY ->
                    "El procedimiento operatorio/restaurador requiere consentimiento informado de odontología general previo a la intervención."
                ConsentType.GENERAL_ADMISSION ->
                    "Se requiere consentimiento informado general de admisión."
            }
            ConsentRequirementStatus.Missing(requiredType, reason)
        }
    }

    private fun determineRequiredConsentType(description: String, toothTreated: Int?): ConsentType? {
        if (description.isBlank() && toothTreated == null) {
            return null
        }

        val bleachingKeywords = listOf(
            "blanqueamiento", "aclaramiento", "peroxido", "peróxido",
            "cubeta", "carbamida", "haz de luz", "blanqueador"
        )
        if (bleachingKeywords.any { description.contains(it) }) {
            return ConsentType.BLEACHING
        }

        val rehabKeywords = listOf(
            "rehabilitacion", "rehabilitación", "protesis", "prótesis", "corona",
            "incrustacion", "incrustación", "rebase", "carilla", "pontico", "póntico",
            "ajuste oclusal", "meryland", "california", "domo", "ceromero", "cerómero",
            "nucleo", "núcleo", "poste", "perno"
        )
        if (rehabKeywords.any { description.contains(it) }) {
            return ConsentType.ORAL_REHABILITATION
        }

        val generalKeywords = listOf(
            "resina", "amalgama", "ionomero", "ionómero", "sellante", "fase higienica",
            "fase higiénica", "profilaxis", "obturacion", "obturación", "urgencia",
            "destartraje", "cavitron", "operatoria", "caries"
        )
        if (generalKeywords.any { description.contains(it) } || toothTreated != null) {
            return ConsentType.GENERAL_ODONTOLOGY
        }

        return null
    }
}
