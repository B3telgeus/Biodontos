package com.example.biodontos.domain.usecase.history

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.EvolutionRepository

class VoidEvolutionUseCase(
    private val evolutionRepository: EvolutionRepository
) {
    suspend operator fun invoke(
        evolution: Evolution,
        reason: String
    ): Either<DomainError, Unit> {
        if (evolution.isVoided) {
            return ClinicalHistoryError.AlreadyVoided.left()
        }
        val cleanReason = reason.trim()
        if (cleanReason.length < MIN_REASON_LENGTH) {
            return ClinicalHistoryError.VoidReasonTooShort(MIN_REASON_LENGTH).left()
        }

        return evolutionRepository.voidEvolution(evolution.id, cleanReason).fold(
            onSuccess = { Unit.right() },
            onFailure = { err ->
                DomainError.NetworkOrPersistenceFailure(
                    err.message ?: "No se pudo anular la evolución clínica."
                ).left()
            }
        )
    }

    companion object {
        const val MIN_REASON_LENGTH = 10
    }
}
