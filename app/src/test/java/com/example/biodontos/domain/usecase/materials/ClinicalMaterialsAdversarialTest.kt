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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClinicalMaterialsAdversarialTest {

    private val materialRepository: MaterialRepository = mockk()
    private val clinicClock: ClinicClock = mockk()

    private lateinit var buildUsageUseCase: BuildEvolutionMaterialUsageUseCase
    private lateinit var saveUseCase: SaveMaterialUseCase

    private val fixedNow = LocalDateTime.of(2026, 10, 30, 10, 0, 0)
    private val today = fixedNow.toLocalDate() // 2026-10-30

    private val baseValidMaterial = ClinicalMaterial(
        id = MaterialId("mat-test-100"),
        doctorId = DoctorId("doc-alpha"),
        name = "Oxido de Zinc Eugenol",
        invimaRegistration = "INVIMA-2022DM-009182",
        batchLotNumber = "LOTE-OZE-881",
        manufacturingDate = LocalDate.of(2024, 5, 10),
        expirationDate = LocalDate.of(2027, 5, 10),
        isActive = true
    )

    @Before
    fun setUp() {
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns fixedNow

        buildUsageUseCase = BuildEvolutionMaterialUsageUseCase(clinicClock)
        saveUseCase = SaveMaterialUseCase(materialRepository, clinicClock)
    }

    // =========================================================================
    // 1. STRESS-TEST: BuildEvolutionMaterialUsageUseCase Boundary Cases
    // =========================================================================

    @Test
    fun `boundary expiration - material expiring on the exact same day as careDate is valid under Colombian clinical law`() = runTest {
        // Colombian clinical & pharmaceutical regulation (INVIMA / Dec. 677/1995):
        // Expiration date indicates validity up to 23:59:59 of that date.
        val expiringToday = baseValidMaterial.copy(expirationDate = today)

        val result = buildUsageUseCase(expiringToday, quantityUsed = 1.0, careDate = today)

        assertTrue("Expected Right for material expiring on careDate", result.isRight())
        result.onRight { snapshot ->
            assertEquals(today.toString(), snapshot.expirationDateSnapshot)
            assertEquals("Oxido de Zinc Eugenol", snapshot.materialNameSnapshot)
        }
    }

    @Test
    fun `boundary expiration - material expiring 1 day before careDate is rejected with MaterialExpired`() = runTest {
        val expiredYesterday = baseValidMaterial.copy(expirationDate = today.minusDays(1))

        val result = buildUsageUseCase(expiredYesterday, quantityUsed = 1.0, careDate = today)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.MaterialExpired, result.leftOrNull())
    }

    @Test
    fun `boundary expiration - material expiring 1 day after careDate is accepted`() = runTest {
        val expiresTomorrow = baseValidMaterial.copy(expirationDate = today.plusDays(1))

        val result = buildUsageUseCase(expiresTomorrow, quantityUsed = 1.0, careDate = today)

        assertTrue(result.isRight())
    }

    @Test
    fun `inactive material with future expiration is rejected with MaterialInactive`() = runTest {
        val inactiveWithFutureExp = baseValidMaterial.copy(
            isActive = false,
            expirationDate = today.plusYears(3)
        )

        val result = buildUsageUseCase(inactiveWithFutureExp, quantityUsed = 1.0, careDate = today)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.MaterialInactive, result.leftOrNull())
    }

    @Test
    fun `inactive material with past expiration prioritizes MaterialInactive`() = runTest {
        val inactiveExpired = baseValidMaterial.copy(
            isActive = false,
            expirationDate = today.minusMonths(6)
        )

        val result = buildUsageUseCase(inactiveExpired, quantityUsed = 1.0, careDate = today)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.MaterialInactive, result.leftOrNull())
    }

    @Test
    fun `quantity stress - zero quantity is rejected with InvalidQuantity`() = runTest {
        val result = buildUsageUseCase(baseValidMaterial, quantityUsed = 0.0, careDate = today)

        assertTrue(result.isLeft())
        assertEquals(MaterialError.InvalidQuantity, result.leftOrNull())
    }

    @Test
    fun `quantity stress - negative quantities are rejected with InvalidQuantity`() = runTest {
        val smallNegative = buildUsageUseCase(baseValidMaterial, quantityUsed = -0.0001, careDate = today)
        assertTrue(smallNegative.isLeft())
        assertEquals(MaterialError.InvalidQuantity, smallNegative.leftOrNull())

        val largeNegative = buildUsageUseCase(baseValidMaterial, quantityUsed = -99999.0, careDate = today)
        assertTrue(largeNegative.isLeft())
        assertEquals(MaterialError.InvalidQuantity, largeNegative.leftOrNull())
    }

    @Test
    fun `quantity stress - extremely large positive quantity is processed by domain`() = runTest {
        val massiveQty = 1_000_000.0
        val result = buildUsageUseCase(baseValidMaterial, quantityUsed = massiveQty, careDate = today)

        assertTrue(result.isRight())
        result.onRight { snapshot ->
            assertEquals(massiveQty, snapshot.quantityUsed, 0.001)
        }
    }

    // =========================================================================
    // 2. STRESS-TEST: SaveMaterialUseCase Boundary Cases
    // =========================================================================

    @Test
    fun `save stress - blank or whitespace-only names are rejected with BlankName`() = runTest {
        val blanks = listOf("", "   ", "\t", "\n", "\r\n  \t  ")

        for (blank in blanks) {
            val material = baseValidMaterial.copy(name = blank)
            val result = saveUseCase(material)
            assertTrue("Expected BlankName for name='$blank'", result.isLeft())
            assertEquals(MaterialError.BlankName, result.leftOrNull())
        }
    }

    @Test
    fun `save stress - blank or whitespace-only invima are rejected with BlankInvimaRegistration`() = runTest {
        val blanks = listOf("", "   ", "\t", "  \n  ")

        for (blank in blanks) {
            val material = baseValidMaterial.copy(invimaRegistration = blank)
            val result = saveUseCase(material)
            assertTrue("Expected BlankInvimaRegistration for invima='$blank'", result.isLeft())
            assertEquals(MaterialError.BlankInvimaRegistration, result.leftOrNull())
        }
    }

    @Test
    fun `save stress - blank or whitespace-only lot numbers are rejected with BlankBatchLotNumber`() = runTest {
        val blanks = listOf("", "   ", "\t", " \r\n ")

        for (blank in blanks) {
            val material = baseValidMaterial.copy(batchLotNumber = blank)
            val result = saveUseCase(material)
            assertTrue("Expected BlankBatchLotNumber for lot='$blank'", result.isLeft())
            assertEquals(MaterialError.BlankBatchLotNumber, result.leftOrNull())
        }
    }

    @Test
    fun `save stress - manufacturing date in the future is rejected with ManufacturingDateInFuture`() = runTest {
        val tomorrow = today.plusDays(1)
        val futureMaterial1 = baseValidMaterial.copy(
            manufacturingDate = tomorrow,
            expirationDate = tomorrow.plusYears(1)
        )
        val result1 = saveUseCase(futureMaterial1)
        assertTrue(result1.isLeft())
        assertEquals(MaterialError.ManufacturingDateInFuture, result1.leftOrNull())

        val tenYearsLater = today.plusYears(10)
        val futureMaterial2 = baseValidMaterial.copy(
            manufacturingDate = tenYearsLater,
            expirationDate = tenYearsLater.plusYears(1)
        )
        val result2 = saveUseCase(futureMaterial2)
        assertTrue(result2.isLeft())
        assertEquals(MaterialError.ManufacturingDateInFuture, result2.leftOrNull())
    }

    @Test
    fun `save boundary - manufacturing date equal to today is allowed`() = runTest {
        val manufacturedToday = baseValidMaterial.copy(
            manufacturingDate = today,
            expirationDate = today.plusYears(2)
        )

        coEvery { materialRepository.saveMaterial(any()) } answers {
            Result.success(firstArg())
        }

        val result = saveUseCase(manufacturedToday)
        assertTrue(result.isRight())
    }

    @Test
    fun `save stress - manufacturing date equal to expiration date is rejected with InvalidDates`() = runTest {
        val sameDate = LocalDate.of(2025, 3, 15)
        val material = baseValidMaterial.copy(
            manufacturingDate = sameDate,
            expirationDate = sameDate
        )

        val result = saveUseCase(material)
        assertTrue(result.isLeft())
        assertEquals(MaterialError.InvalidDates, result.leftOrNull())
    }

    @Test
    fun `save stress - manufacturing date after expiration date is rejected with InvalidDates`() = runTest {
        val mfg = LocalDate.of(2025, 6, 1)
        val exp = LocalDate.of(2025, 5, 31)
        val invertedMaterial = baseValidMaterial.copy(
            manufacturingDate = mfg,
            expirationDate = exp
        )

        val result = saveUseCase(invertedMaterial)
        assertTrue(result.isLeft())
        assertEquals(MaterialError.InvalidDates, result.leftOrNull())
    }

    @Test
    fun `save boundary - expiration date 1 day after manufacturing date is allowed`() = runTest {
        val mfg = LocalDate.of(2025, 1, 1)
        val exp = LocalDate.of(2025, 1, 2)
        val material = baseValidMaterial.copy(
            manufacturingDate = mfg,
            expirationDate = exp
        )

        coEvery { materialRepository.saveMaterial(any()) } answers {
            Result.success(firstArg())
        }

        val result = saveUseCase(material)
        assertTrue(result.isRight())
    }

    @Test
    fun `save stress - inputs with surrounding whitespace are trimmed before persistence`() = runTest {
        val material = baseValidMaterial.copy(
            name = "   Ionómero de Vidrio Tipo II   ",
            invimaRegistration = "   INVIMA-2023DM-00123   ",
            batchLotNumber = "   LOTE-9988   "
        )

        coEvery { materialRepository.saveMaterial(any()) } answers {
            val passed = firstArg<ClinicalMaterial>()
            Result.success(passed)
        }

        val result = saveUseCase(material)
        assertTrue(result.isRight())
        result.onRight { saved ->
            assertEquals("Ionómero de Vidrio Tipo II", saved.name)
            assertEquals("INVIMA-2023DM-00123", saved.invimaRegistration)
            assertEquals("LOTE-9988", saved.batchLotNumber)
        }
    }

    // =========================================================================
    // 3. STRESS-TEST: Value Classes and Domain Invariants
    // =========================================================================

    @Test(expected = IllegalArgumentException::class)
    fun `MaterialId with blank or whitespace string throws IllegalArgumentException`() {
        MaterialId("    ")
    }

    @Test
    fun `ClinicalMaterial isNearExpiration boundary behavior`() {
        val refDate = LocalDate.of(2026, 10, 30)

        // Expiring today (0 days remaining) -> should be near expiration, NOT expired
        val expiringToday = baseValidMaterial.copy(expirationDate = refDate)
        assertFalse(expiringToday.isExpired(refDate))
        assertTrue(expiringToday.isNearExpiration(refDate, thresholdDays = 30))

        // Expiring in exactly 30 days -> near expiration
        val in30Days = baseValidMaterial.copy(expirationDate = refDate.plusDays(30))
        assertTrue(in30Days.isNearExpiration(refDate, thresholdDays = 30))

        // Expiring in 31 days -> not near expiration
        val in31Days = baseValidMaterial.copy(expirationDate = refDate.plusDays(31))
        assertFalse(in31Days.isNearExpiration(refDate, thresholdDays = 30))

        // Expired yesterday -> isNearExpiration must be false
        val expired = baseValidMaterial.copy(expirationDate = refDate.minusDays(1))
        assertTrue(expired.isExpired(refDate))
        assertFalse(expired.isNearExpiration(refDate, thresholdDays = 30))
    }
}
