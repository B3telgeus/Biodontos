package com.example.biodontos.domain.usecase.history

import com.example.biodontos.domain.model.ClinicalHistoryError
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.history.VoidInfo
import com.example.biodontos.domain.repository.EvolutionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidEvolutionUseCaseTest {

    private val evolutionRepository: EvolutionRepository = mockk()
    private val useCase = VoidEvolutionUseCase(evolutionRepository)

    private val activeEvolution = Evolution(
        id = "evo-10",
        recordId = "rec-1",
        doctorId = "doc-1",
        date = "2026-10-20",
        procedureTime = "10:00:00",
        description = "Exodoncia simple"
    )

    @Test
    fun `rejects reason shorter than 10 characters`() = runTest {
        val result = useCase(activeEvolution, "Corto")
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ClinicalHistoryError.VoidReasonTooShort)
    }

    @Test
    fun `rejects evolution that is already voided`() = runTest {
        val voidedEvo = activeEvolution.copy(
            voidInfo = VoidInfo(LocalDateTime.now(), "Anulado previamente por error")
        )
        val result = useCase(voidedEvo, "Nuevo intento de anulación")
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ClinicalHistoryError.AlreadyVoided)
    }

    @Test
    fun `succeeds when reason has at least 10 characters and evolution is active`() = runTest {
        coEvery {
            evolutionRepository.voidEvolution("evo-10", "Error en selección de diente FDI")
        } returns Result.success(Unit)

        val result = useCase(activeEvolution, "  Error en selección de diente FDI  ")
        assertTrue(result.isRight())
        coVerify(exactly = 1) {
            evolutionRepository.voidEvolution("evo-10", "Error en selección de diente FDI")
        }
    }
}
