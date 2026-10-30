package com.example.biodontos.domain.usecase.materials

import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialError
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.repository.MaterialRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import io.mockk.coEvery
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

class SaveMaterialUseCaseTest {

    private val materialRepository: MaterialRepository = mockk()
    private val clinicClock: ClinicClock = mockk()
    private lateinit var useCase: SaveMaterialUseCase

    private val fixedNow = LocalDateTime.of(2026, 10, 30, 10, 0)

    @Before
    fun setUp() {
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns fixedNow
        useCase = SaveMaterialUseCase(materialRepository, clinicClock)
    }

    @Test
    fun `save with blank name returns BlankName error`() = runTest {
        val material = ClinicalMaterial(
            name = "   ",
            invimaRegistration = "INV-123",
            batchLotNumber = "LOT-1",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = LocalDate.of(2027, 1, 1)
        )

        val result = useCase(material)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.BlankName, result.leftOrNull())
    }

    @Test
    fun `save with blank invima returns BlankInvimaRegistration error`() = runTest {
        val material = ClinicalMaterial(
            name = "Resina Z350",
            invimaRegistration = "",
            batchLotNumber = "LOT-1",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = LocalDate.of(2027, 1, 1)
        )

        val result = useCase(material)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.BlankInvimaRegistration, result.leftOrNull())
    }

    @Test
    fun `save with blank lot number returns BlankBatchLotNumber error`() = runTest {
        val material = ClinicalMaterial(
            name = "Resina Z350",
            invimaRegistration = "INV-123",
            batchLotNumber = " \t ",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = LocalDate.of(2027, 1, 1)
        )

        val result = useCase(material)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.BlankBatchLotNumber, result.leftOrNull())
    }

    @Test
    fun `save with manufacturing date in future returns ManufacturingDateInFuture error`() = runTest {
        val futureDate = fixedNow.toLocalDate().plusDays(1)
        val material = ClinicalMaterial(
            name = "Resina Z350",
            invimaRegistration = "INV-123",
            batchLotNumber = "LOT-1",
            manufacturingDate = futureDate,
            expirationDate = futureDate.plusYears(2)
        )

        val result = useCase(material)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.ManufacturingDateInFuture, result.leftOrNull())
    }

    @Test
    fun `save with expiration date before or equal to manufacturing date returns InvalidDates error`() = runTest {
        val mfgDate = LocalDate.of(2024, 1, 1)

        val equalDates = ClinicalMaterial(
            name = "Resina Z350",
            invimaRegistration = "INV-123",
            batchLotNumber = "LOT-1",
            manufacturingDate = mfgDate,
            expirationDate = mfgDate
        )
        val resultEqual = useCase(equalDates)
        assertTrue(resultEqual.isLeft())
        assertEquals(MaterialError.InvalidDates, resultEqual.leftOrNull())

        val invertedDates = ClinicalMaterial(
            name = "Resina Z350",
            invimaRegistration = "INV-123",
            batchLotNumber = "LOT-1",
            manufacturingDate = mfgDate,
            expirationDate = mfgDate.minusDays(1)
        )
        val resultInverted = useCase(invertedDates)
        assertTrue(resultInverted.isLeft())
        assertEquals(MaterialError.InvalidDates, resultInverted.leftOrNull())
    }

    @Test
    fun `save with valid attributes trims whitespace and returns saved material`() = runTest {
        val material = ClinicalMaterial(
            id = MaterialId("mat-valid"),
            doctorId = DoctorId("doc-1"),
            name = "  Resina Filtek Z350 XT  ",
            invimaRegistration = "  INVIMA 2018DM-0018241  ",
            batchLotNumber = "  LOTE-9921A  ",
            manufacturingDate = LocalDate.of(2024, 1, 10),
            expirationDate = LocalDate.of(2027, 1, 10),
            isActive = true
        )

        coEvery { materialRepository.saveMaterial(any()) } answers {
            val passed = firstArg<ClinicalMaterial>()
            Result.success(passed)
        }

        val result = useCase(material)

        assertTrue(result.isRight())
        result.onRight { saved ->
            assertEquals("Resina Filtek Z350 XT", saved.name)
            assertEquals("INVIMA 2018DM-0018241", saved.invimaRegistration)
            assertEquals("LOTE-9921A", saved.batchLotNumber)
        }
    }

    @Test
    fun `save returns DuplicateMaterial when repository reports unique constraint violation`() = runTest {
        val material = ClinicalMaterial(
            name = "Resina Filtek",
            invimaRegistration = "INVIMA-123",
            batchLotNumber = "LOTE-1",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = LocalDate.of(2027, 1, 1)
        )

        coEvery { materialRepository.saveMaterial(any()) } returns Result.failure(
            RuntimeException("duplicate key value violates unique constraint ux_clinical_materials_doctor_invima_lot_active (SQLSTATE 23505)")
        )

        val result = useCase(material)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.DuplicateMaterial, result.leftOrNull())
    }

    @Test
    fun `save returns PersistenceError on generic repository failure`() = runTest {
        val material = ClinicalMaterial(
            name = "Resina Filtek",
            invimaRegistration = "INVIMA-123",
            batchLotNumber = "LOTE-1",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = LocalDate.of(2027, 1, 1)
        )

        coEvery { materialRepository.saveMaterial(any()) } returns Result.failure(
            RuntimeException("Database disk full")
        )

        val result = useCase(material)

        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is MaterialError.PersistenceError)
            val persistenceError = error as MaterialError.PersistenceError
            assertTrue(persistenceError.reason.contains("Database disk full"))
        }
    }
}
