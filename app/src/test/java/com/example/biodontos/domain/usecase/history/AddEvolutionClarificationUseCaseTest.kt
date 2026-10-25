package com.example.biodontos.domain.usecase.history

import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.repository.EvolutionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEvolutionClarificationUseCaseTest {

    private val evolutionRepository: EvolutionRepository = mockk()
    private val useCase = AddEvolutionClarificationUseCase(evolutionRepository)

    private val originalEvolution = Evolution(
        id = "evo-original",
        recordId = "rec-1",
        doctorId = "doc-1",
        date = "2026-10-20",
        procedureTime = "10:00:00",
        description = "Obturación oclusal en diente 26"
    )

    @Test
    fun `rejects blank clarification text`() = runTest {
        val result = useCase(originalEvolution, "   ", "doc-1", "Dr. Gómez")
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ClinicalHistoryError.ClarificationEmpty)
    }

    @Test
    fun `creates linked clarification note preserving original evolution`() = runTest {
        val savedClarification = originalEvolution.copy(
            id = "evo-clarif-1",
            description = "NOTA ACLARATORIA: El diente tratado corresponde al 27",
            amendsEvolutionId = "evo-original"
        )
        coEvery {
            evolutionRepository.saveClarification(
                originalEvolution = originalEvolution,
                clarificationText = "El diente tratado corresponde al 27",
                doctorId = "doc-1",
                doctorName = "Dr. Gómez"
            )
        } returns Result.success(savedClarification)

        val result = useCase(
            originalEvolution = originalEvolution,
            clarificationText = "  El diente tratado corresponde al 27  ",
            doctorId = "doc-1",
            doctorName = "Dr. Gómez"
        )

        assertTrue(result.isRight())
        assertEquals("evo-original", result.getOrNull()?.amendsEvolutionId)
        coVerify(exactly = 1) {
            evolutionRepository.saveClarification(
                originalEvolution = originalEvolution,
                clarificationText = "El diente tratado corresponde al 27",
                doctorId = "doc-1",
                doctorName = "Dr. Gómez"
            )
        }
    }
}
