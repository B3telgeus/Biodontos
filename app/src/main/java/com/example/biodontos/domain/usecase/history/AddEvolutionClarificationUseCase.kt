package com.example.biodontos.domain.usecase.history

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.EvolutionRepository

class AddEvolutionClarificationUseCase(
    private val evolutionRepository: EvolutionRepository
) {
    suspend operator fun invoke(
        originalEvolution: Evolution,
        clarificationText: String,
        doctorId: String,
        doctorName: String? = null
    ): Either<DomainError, Evolution> {
        val cleanText = clarificationText.trim()
        if (cleanText.isBlank()) {
            return ClinicalHistoryError.ClarificationEmpty.left()
        }

        return evolutionRepository.saveClarification(
            originalEvolution = originalEvolution,
            clarificationText = cleanText,
            doctorId = doctorId,
            doctorName = doctorName
        ).fold(
            onSuccess = { it.right() },
            onFailure = { err ->
                DomainError.NetworkOrPersistenceFailure(
                    err.message ?: "No se pudo registrar la nota aclaratoria."
                ).left()
            }
        )
    }
}
