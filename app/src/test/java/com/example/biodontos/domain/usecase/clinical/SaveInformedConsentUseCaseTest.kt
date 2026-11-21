package com.example.biodontos.domain.usecase.clinical

import com.example.biodontos.domain.model.ConsentType
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.repository.InformedConsentRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveInformedConsentUseCaseTest {

    private val repository: InformedConsentRepository = mockk()
    private val useCase = SaveInformedConsentUseCase(repository)

    @Test
    fun `fails when patientId is blank`() = runTest {
        val consent = InformedConsent(
            patientId = "",
            doctorId = "doc-1",
            treatmentSummary = "Tratamiento"
        )
        val result = useCase(consent)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `fails when doctorId is blank`() = runTest {
        val consent = InformedConsent(
            patientId = "pat-1",
            doctorId = "",
            treatmentSummary = "Tratamiento"
        )
        val result = useCase(consent)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `fails when neither patient nor guardian signature is present`() = runTest {
        val consent = InformedConsent(
            patientId = "pat-1",
            doctorId = "doc-1",
            treatmentSummary = "Tratamiento",
            patientSignatureUrl = null,
            guardianSignatureUrl = null,
            doctorSignatureUrl = "data:image/png;base64,doc"
        )
        val result = useCase(consent)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("firma del paciente o de su acudiente")
    }

    @Test
    fun `fails when doctor signature is missing`() = runTest {
        val consent = InformedConsent(
            patientId = "pat-1",
            doctorId = "doc-1",
            treatmentSummary = "Tratamiento",
            patientSignatureUrl = "data:image/png;base64,pat",
            doctorSignatureUrl = null
        )
        val result = useCase(consent)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("firma y sello del odontólogo")
    }

    @Test
    fun `fails when oral rehabilitation consent has not accepted occlusion trial`() = runTest {
        val consent = InformedConsent(
            patientId = "pat-1",
            doctorId = "doc-1",
            consentType = ConsentType.ORAL_REHABILITATION,
            treatmentSummary = "Corona metal porcelana",
            patientSignatureUrl = "data:image/png;base64,pat",
            doctorSignatureUrl = "data:image/png;base64,doc",
            occlusionTestAccepted = false
        )
        val result = useCase(consent)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("prueba de oclusión")
    }

    @Test
    fun `succeeds and calls repository when all validation criteria are met`() = runTest {
        val validConsent = InformedConsent(
            patientId = "pat-1",
            doctorId = "doc-1",
            consentType = ConsentType.ORAL_REHABILITATION,
            treatmentSummary = "Corona metal porcelana",
            patientSignatureUrl = "data:image/png;base64,pat",
            doctorSignatureUrl = "data:image/png;base64,doc",
            occlusionTestAccepted = true
        )
        coEvery { repository.saveConsent(validConsent) } returns Result.success(validConsent)

        val result = useCase(validConsent)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(validConsent)
        coVerify(exactly = 1) { repository.saveConsent(validConsent) }
    }
}
