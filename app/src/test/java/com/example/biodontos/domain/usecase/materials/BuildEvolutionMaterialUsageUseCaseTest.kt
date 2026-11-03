package com.example.biodontos.domain.usecase.materials

import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialError
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.scheduling.ClinicClock
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildEvolutionMaterialUsageUseCaseTest {

    private val clinicClock: ClinicClock = mockk()
    private lateinit var useCase: BuildEvolutionMaterialUsageUseCase

    private val fixedNow = LocalDateTime.of(2026, 10, 30, 9, 30)
    private val today = fixedNow.toLocalDate()

    private val activeMaterial = ClinicalMaterial(
        id = MaterialId("mat-composite-1"),
        doctorId = DoctorId("doc-1"),
        name = "Resina Brilliant EverGlow",
        invimaRegistration = "INVIMA-COL-9901",
        batchLotNumber = "LOTE-BEG-44",
        manufacturingDate = LocalDate.of(2024, 6, 1),
        expirationDate = LocalDate.of(2027, 6, 1),
        isActive = true
    )

    @Before
    fun setUp() {
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns fixedNow
        useCase = BuildEvolutionMaterialUsageUseCase(clinicClock)
    }

    @Test
    fun `inactive material returns MaterialInactive error`() = runTest {
        val inactive = activeMaterial.copy(isActive = false)

        val result = useCase(inactive, quantityUsed = 1.0)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.MaterialInactive, result.leftOrNull())
    }

    @Test
    fun `zero or negative quantity returns InvalidQuantity error`() = runTest {
        val zeroResult = useCase(activeMaterial, quantityUsed = 0.0)
        assertTrue(zeroResult.isLeft())
        assertEquals(MaterialError.InvalidQuantity, zeroResult.leftOrNull())

        val negativeResult = useCase(activeMaterial, quantityUsed = -2.5)
        assertTrue(negativeResult.isLeft())
        assertEquals(MaterialError.InvalidQuantity, negativeResult.leftOrNull())
    }

    @Test
    fun `expired material at careDate returns MaterialExpired error`() = runTest {
        val expired = activeMaterial.copy(
            expirationDate = today.minusDays(1)
        )

        val result = useCase(expired, quantityUsed = 1.0, careDate = today)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.MaterialExpired, result.leftOrNull())
    }

    @Test
    fun `material expiring on careDate is accepted and not expired`() = runTest {
        val expiringToday = activeMaterial.copy(
            expirationDate = today
        )

        val result = useCase(expiringToday, quantityUsed = 1.0, careDate = today)

        assertTrue(result.isRight())
        result.onRight { snapshot ->
            assertEquals(today.toString(), snapshot.expirationDateSnapshot)
        }
    }

    @Test
    fun `valid active material builds full immutable evolution snapshot`() = runTest {
        val result = useCase(activeMaterial, quantityUsed = 2.5)

        assertTrue(result.isRight())
        result.onRight { snapshot ->
            assertEquals("mat-composite-1", snapshot.materialId)
            assertEquals("Resina Brilliant EverGlow", snapshot.materialName)
            assertEquals("Resina Brilliant EverGlow", snapshot.materialNameSnapshot)
            assertEquals(2.5, snapshot.quantityUsed, 0.001)
            assertEquals("LOTE-BEG-44", snapshot.lotNumberSnapshot)
            assertEquals("INVIMA-COL-9901", snapshot.invimaSnapshot)
            assertEquals("2024-06-01", snapshot.manufacturingDateSnapshot)
            assertEquals("2027-06-01", snapshot.expirationDateSnapshot)
        }
    }
}
