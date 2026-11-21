package com.example.biodontos.domain.usecase.clinical

import com.example.biodontos.domain.model.ConsentType
import com.example.biodontos.domain.model.InformedConsent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidateEvolutionConsentRequirementUseCaseTest {

    private val useCase = ValidateEvolutionConsentRequirementUseCase()

    @Test
    fun `returns NotRequired when description is blank and tooth is null`() {
        val status = useCase(description = "", toothTreated = null, consents = emptyList())
        assertThat(status).isInstanceOf(ConsentRequirementStatus.NotRequired::class.java)
    }

    @Test
    fun `detects Bleaching consent requirement and reports Missing when no consent exists`() {
        val status = useCase(
            description = "Aclaramiento dental en consultorio con peróxido de hidrógeno al 35%",
            toothTreated = null,
            consents = emptyList()
        )
        assertThat(status).isInstanceOf(ConsentRequirementStatus.Missing::class.java)
        val missing = status as ConsentRequirementStatus.Missing
        assertThat(missing.requiredType).isEqualTo(ConsentType.BLEACHING)
    }

    @Test
    fun `detects Bleaching consent and reports Satisfied when signed consent exists`() {
        val bleachingConsent = InformedConsent(
            id = "c-bleach-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            consentType = ConsentType.BLEACHING,
            treatmentSummary = "Blanqueamiento dental",
            status = "SIGNED",
            patientSignatureUrl = "data:image/png;base64,sample"
        )
        val status = useCase(
            description = "Sesión 2 de blanqueamiento dental",
            toothTreated = null,
            consents = listOf(bleachingConsent)
        )
        assertThat(status).isInstanceOf(ConsentRequirementStatus.Satisfied::class.java)
        val satisfied = status as ConsentRequirementStatus.Satisfied
        assertThat(satisfied.consent.id).isEqualTo("c-bleach-1")
        assertThat(satisfied.type).isEqualTo(ConsentType.BLEACHING)
    }

    @Test
    fun `detects Oral Rehabilitation requirement for crown and reports Missing when missing`() {
        val status = useCase(
            description = "Toma de impresión para corona de metal porcelana en diente 26",
            toothTreated = 26,
            consents = emptyList()
        )
        assertThat(status).isInstanceOf(ConsentRequirementStatus.Missing::class.java)
        val missing = status as ConsentRequirementStatus.Missing
        assertThat(missing.requiredType).isEqualTo(ConsentType.ORAL_REHABILITATION)
    }

    @Test
    fun `detects General Odontology requirement for resin and reports Satisfied when signed`() {
        val generalConsent = InformedConsent(
            id = "c-gen-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            consentType = ConsentType.GENERAL_ODONTOLOGY,
            treatmentSummary = "Odontología general",
            status = "SIGNED",
            patientSignatureUrl = "data:image/png;base64,sample"
        )
        val status = useCase(
            description = "Restauración cavidad oclusal con resina compuesta",
            toothTreated = 16,
            consents = listOf(generalConsent)
        )
        assertThat(status).isInstanceOf(ConsentRequirementStatus.Satisfied::class.java)
        val satisfied = status as ConsentRequirementStatus.Satisfied
        assertThat(satisfied.consent.id).isEqualTo("c-gen-1")
        assertThat(satisfied.type).isEqualTo(ConsentType.GENERAL_ODONTOLOGY)
    }

    @Test
    fun `reports Missing when consent exists but signature is missing`() {
        val unsignedConsent = InformedConsent(
            id = "c-gen-unsigned",
            patientId = "pat-1",
            doctorId = "doc-1",
            consentType = ConsentType.GENERAL_ODONTOLOGY,
            treatmentSummary = "Odontología general",
            status = "SIGNED",
            patientSignatureUrl = null,
            guardianSignatureUrl = null
        )
        val status = useCase(
            description = "Aplicación de sellante",
            toothTreated = 46,
            consents = listOf(unsignedConsent)
        )
        assertThat(status).isInstanceOf(ConsentRequirementStatus.Missing::class.java)
    }
}
