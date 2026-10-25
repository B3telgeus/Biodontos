package com.example.biodontos.domain.usecase.odontogram

import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface
import com.example.biodontos.domain.repository.OdontogramRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OdontogramUseCasesTest {

    private val repository: OdontogramRepository = mockk()

    @Test
    fun `GetOdontogramUseCase returns tooth list for patient`() = runTest {
        val teeth = listOf(
            Tooth(number = 11, surfaceStates = mapOf(ToothSurface.VESTIBULAR to ToothCondition.SANO))
        )
        coEvery { repository.getOdontogram("pat-1") } returns Result.success(teeth)

        val useCase = GetOdontogramUseCase(repository)
        val result = useCase("pat-1")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrDefault(emptyList()).size)
    }

    @Test
    fun `UpdateToothUseCase delegates surface update to repository`() = runTest {
        coEvery {
            repository.updateToothState(
                patientId = "pat-1",
                toothNumber = 26,
                surface = ToothSurface.OCLUSAL,
                condition = ToothCondition.CARIES,
                comment = "Caries profunda"
            )
        } returns Result.success(Unit)

        val useCase = UpdateToothUseCase(repository)
        val result = useCase(
            patientId = "pat-1",
            toothNumber = 26,
            surface = ToothSurface.OCLUSAL,
            condition = ToothCondition.CARIES,
            comment = "Caries profunda"
        )

        assertTrue(result.isSuccess)
    }
}
