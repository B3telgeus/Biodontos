package com.example.biodontos.domain.usecase.clinical

import com.example.biodontos.domain.model.ConsentType
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.repository.InformedConsentRepository

class SaveInformedConsentUseCase(
    private val repository: InformedConsentRepository
) {
    suspend operator fun invoke(consent: InformedConsent): Result<InformedConsent> {
        if (consent.patientId.isBlank()) {
            return Result.failure(IllegalArgumentException("El consentimiento requiere un paciente válido."))
        }
        if (consent.doctorId.isBlank()) {
            return Result.failure(IllegalArgumentException("El consentimiento requiere un odontólogo tratante asignado."))
        }

        val hasPatientOrGuardianSignature = !consent.patientSignatureUrl.isNullOrBlank() ||
            !consent.guardianSignatureUrl.isNullOrBlank()
        if (!hasPatientOrGuardianSignature) {
            return Result.failure(IllegalStateException("Debe registrarse la firma del paciente o de su acudiente legal."))
        }

        if (consent.doctorSignatureUrl.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Debe registrarse la firma y sello del odontólogo tratante."))
        }

        if (consent.consentType == ConsentType.ORAL_REHABILITATION && !consent.occlusionTestAccepted) {
            return Result.failure(IllegalStateException("Para rehabilitación oral es obligatoria la aceptación de la prueba de oclusión y estética."))
        }

        return repository.saveConsent(consent)
    }
}
