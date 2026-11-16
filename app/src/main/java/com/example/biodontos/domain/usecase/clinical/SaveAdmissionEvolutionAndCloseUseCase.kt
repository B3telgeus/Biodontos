package com.example.biodontos.domain.usecase.clinical

import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import kotlinx.coroutines.CancellationException

class SaveAdmissionEvolutionAndCloseUseCase(
    private val evolutionRepository: EvolutionRepository,
    private val clinicalRecordRepository: ClinicalRecordRepository? = null
) {
    suspend operator fun invoke(
        evolution: Evolution,
        closeRecord: Boolean = false,
        closureSummary: String? = null,
        closureSignatureUrl: String? = null
    ): Result<Evolution> {
        val effectiveSummary = closureSummary?.ifBlank { "Tratamiento de ingreso concluido." }
            ?: "Tratamiento de ingreso concluido."
        val effectiveSignature = closureSignatureUrl?.ifBlank { null }

        val saveResult = try {
            val candidateResult = evolutionRepository.saveEvolution(
                evolution = evolution,
                closeRecord = closeRecord,
                closureSummary = if (closeRecord) effectiveSummary else null,
                closureSignatureUrl = if (closeRecord) effectiveSignature else null
            )
            if (candidateResult.isSuccess) {
                val evo = candidateResult.getOrNull()
                if (evo !is Evolution) {
                    evolutionRepository.saveEvolution(evolution)
                } else {
                    candidateResult
                }
            } else {
                val exception = candidateResult.exceptionOrNull()
                if (exception == null) {
                    evolutionRepository.saveEvolution(evolution)
                } else {
                    candidateResult
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            evolutionRepository.saveEvolution(evolution)
        }

        saveResult.onSuccess {
            if (closeRecord && clinicalRecordRepository != null && evolution.recordId.isNotBlank()) {
                clinicalRecordRepository.closeRecord(
                    recordId = evolution.recordId,
                    doctorId = evolution.doctorId,
                    summary = effectiveSummary,
                    signatureUrl = closureSignatureUrl.orEmpty()
                )
            }
        }

        return saveResult
    }
}
