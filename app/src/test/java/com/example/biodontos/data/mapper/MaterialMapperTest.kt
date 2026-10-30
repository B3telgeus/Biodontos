package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.ClinicalMaterialDto
import com.example.biodontos.data.dto.EvolutionMaterialUsedDto
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.MaterialError
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.scheduling.ClinicClock
import com.example.biodontos.domain.usecase.materials.BuildEvolutionMaterialUsageUseCase
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialMapperTest {

    @Test
    fun `ClinicalMaterialDto toDomain maps all fields correctly`() {
        val dto = ClinicalMaterialDto(
            id = "00000000-0000-0000-0000-000000000001",
            doctorId = "doc-456",
            name = "Ionómero de Vidrio",
            invimaRegistration = "INVIMA 2021DM-009876",
            batchLotNumber = "LOT-2024-X",
            manufacturingDate = "2024-03-15",
            expirationDate = "2026-03-15",
            isActive = true,
            createdAt = "2024-03-15T10:00:00Z",
            updatedAt = "2024-03-16T12:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("00000000-0000-0000-0000-000000000001", domain.id.value)
        assertEquals("doc-456", domain.doctorId?.value)
        assertEquals("Ionómero de Vidrio", domain.name)
        assertEquals("INVIMA 2021DM-009876", domain.invimaRegistration)
        assertEquals("LOT-2024-X", domain.batchLotNumber)
        assertEquals(LocalDate.of(2024, 3, 15), domain.manufacturingDate)
        assertEquals(LocalDate.of(2026, 3, 15), domain.expirationDate)
        assertTrue(domain.isActive)
        assertEquals(Instant.parse("2024-03-15T10:00:00Z"), domain.createdAt)
        assertEquals(Instant.parse("2024-03-16T12:00:00Z"), domain.updatedAt)
    }

    @Test
    fun `ClinicalMaterialDto toDomain handles null id and doctorId safely`() {
        val dto = ClinicalMaterialDto(
            id = null,
            doctorId = null,
            name = "Anestesia Lidocaína 2%",
            invimaRegistration = "INVIMA-112233",
            batchLotNumber = "LOT-77",
            manufacturingDate = "2023-01-01",
            expirationDate = "2025-01-01",
            isActive = false,
            createdAt = null,
            updatedAt = null
        )

        val domain = dto.toDomain()

        assertTrue(domain.id.value.isNotBlank())
        assertNull(domain.doctorId)
        assertEquals("Anestesia Lidocaína 2%", domain.name)
        assertFalse(domain.isActive)
        assertNull(domain.createdAt)
        assertNull(domain.updatedAt)
    }

    @Test
    fun `ClinicalMaterial toDto maps all fields back accurately`() {
        val material = ClinicalMaterial(
            id = MaterialId("mat-999"),
            doctorId = DoctorId("doc-123"),
            name = "Grabado Ácido Fosfórico 37%",
            invimaRegistration = "INVIMA-ACID-99",
            batchLotNumber = "LOTE-AC-1",
            manufacturingDate = LocalDate.of(2024, 5, 10),
            expirationDate = LocalDate.of(2027, 5, 10),
            isActive = true,
            createdAt = Instant.parse("2024-05-10T08:00:00Z"),
            updatedAt = Instant.parse("2024-05-10T08:30:00Z")
        )

        val dto = material.toDto()

        assertEquals("mat-999", dto.id)
        assertEquals("doc-123", dto.doctorId)
        assertEquals("Grabado Ácido Fosfórico 37%", dto.name)
        assertEquals("INVIMA-ACID-99", dto.invimaRegistration)
        assertEquals("LOTE-AC-1", dto.batchLotNumber)
        assertEquals("2024-05-10", dto.manufacturingDate)
        assertEquals("2027-05-10", dto.expirationDate)
        assertTrue(dto.isActive)
        assertEquals("2024-05-10T08:00:00Z", dto.createdAt)
        assertEquals("2024-05-10T08:30:00Z", dto.updatedAt)
    }

    @Test
    fun `EvolutionMaterialUsedDto toDomain preserves snapshot name and never sets empty string`() {
        val dtoWithSnapshot = EvolutionMaterialUsedDto(
            id = "emu-1",
            evolutionId = "evo-10",
            materialId = "mat-1",
            materialNameSnapshot = "Resina Z350 XT",
            quantityUsed = 1.5,
            lotNumberSnapshot = "LOT-1",
            invimaSnapshot = "INV-1",
            expirationDateSnapshot = "2026-12-31",
            manufacturingDateSnapshot = "2024-01-01"
        )
        val domainWithSnapshot = dtoWithSnapshot.toDomain()
        assertEquals("Resina Z350 XT", domainWithSnapshot.materialName)
        assertEquals("Resina Z350 XT", domainWithSnapshot.materialNameSnapshot)
        assertEquals("2024-01-01", domainWithSnapshot.manufacturingDateSnapshot)

        val dtoWithoutSnapshot = EvolutionMaterialUsedDto(
            id = "emu-2",
            evolutionId = "evo-10",
            materialId = "mat-2",
            materialNameSnapshot = null,
            quantityUsed = 1.0,
            lotNumberSnapshot = "LOT-2",
            invimaSnapshot = "INV-2",
            expirationDateSnapshot = "2026-12-31",
            manufacturingDateSnapshot = null
        )
        val domainWithoutSnapshot = dtoWithoutSnapshot.toDomain()
        assertEquals("Material Clínico", domainWithoutSnapshot.materialName)
        assertEquals("Material Clínico", domainWithoutSnapshot.materialNameSnapshot)
        assertEquals("", domainWithoutSnapshot.manufacturingDateSnapshot)
    }

    @Test
    fun `EvolutionMaterialUsed toDto populates snapshot columns`() {
        val domain = EvolutionMaterialUsed(
            id = "emu-1",
            materialId = "mat-1",
            materialName = "Sellante de Fosas y Fisuras",
            quantityUsed = 2.0,
            lotNumberSnapshot = "LOT-SELL",
            invimaSnapshot = "INV-SELL",
            expirationDateSnapshot = "2027-06-30",
            materialNameSnapshot = "Sellante de Fosas y Fisuras",
            manufacturingDateSnapshot = "2024-06-30"
        )

        val dto = domain.toDto("evo-99")

        assertEquals("emu-1", dto.id)
        assertEquals("evo-99", dto.evolutionId)
        assertEquals("mat-1", dto.materialId)
        assertEquals("Sellante de Fosas y Fisuras", dto.materialNameSnapshot)
        assertEquals(2.0, dto.quantityUsed, 0.001)
        assertEquals("LOT-SELL", dto.lotNumberSnapshot)
        assertEquals("INV-SELL", dto.invimaSnapshot)
        assertEquals("2027-06-30", dto.expirationDateSnapshot)
        assertEquals("2024-06-30", dto.manufacturingDateSnapshot)
    }

    // =========================================================================
    // ADVERSARIAL STRESS TESTS: DATE PARSING & CORRUPT INPUT RESILIENCE
    // =========================================================================

    @Test
    fun `ClinicalMaterialDto toDomain handles corrupt, malformed and empty dates safely without crashing`() {
        val corruptManufacturingDates = listOf(
            "",
            "   ",
            "\t\n",
            "not-a-date",
            "2024-13-45",
            "2024-02-30",
            "2025-02-29", // Non-leap year
            "2024/05/10", // Slash format
            "10-05-2024", // DD-MM-YYYY
            "2024-5-1",   // Short month/day
            "📅2024-01-01",
            "null",
            "undefined",
            "+1000000000-01-01",
            "2024"
        )

        for (corruptDate in corruptManufacturingDates) {
            val dto = ClinicalMaterialDto(
                id = "mat-test",
                doctorId = "doc-1",
                name = "Test Material",
                invimaRegistration = "INV-123",
                batchLotNumber = "LOT-1",
                manufacturingDate = corruptDate,
                expirationDate = "2027-12-31"
            )

            val domain = dto.toDomain()
            assertNotNull("Domain manufacturingDate must not be null for '$corruptDate'", domain.manufacturingDate)
            assertEquals("Default fallback must be today for invalid date '$corruptDate'", LocalDate.now(), domain.manufacturingDate)
        }

        val corruptExpirationDates = listOf(
            "",
            "   ",
            "\t\n",
            "invalid-exp",
            "2026-11-31", // Nov has 30 days
            "2024-00-00",
            "9999-99-99",
            "2024.12.31",
            "31/12/2026",
            "EXP-SOON"
        )

        for (corruptExp in corruptExpirationDates) {
            val dto = ClinicalMaterialDto(
                id = "mat-test",
                doctorId = "doc-1",
                name = "Test Material",
                invimaRegistration = "INV-123",
                batchLotNumber = "LOT-1",
                manufacturingDate = "2024-01-01",
                expirationDate = corruptExp
            )

            val domain = dto.toDomain()
            assertNotNull("Domain expirationDate must not be null for '$corruptExp'", domain.expirationDate)
            assertEquals(
                "Default fallback must be 1 year ahead for invalid date '$corruptExp'",
                LocalDate.now().plusYears(1),
                domain.expirationDate
            )
        }
    }

    @Test
    fun `ClinicalMaterialDto toDomain accurately parses ISO strings with time, timezone offset and leap years`() {
        val leapYearDto = ClinicalMaterialDto(
            id = "mat-leap",
            name = "Leap Material",
            invimaRegistration = "INV-LEAP",
            batchLotNumber = "LOT-LEAP",
            manufacturingDate = "2024-02-29",
            expirationDate = "2028-02-29"
        )
        val leapDomain = leapYearDto.toDomain()
        assertEquals(LocalDate.of(2024, 2, 29), leapDomain.manufacturingDate)
        assertEquals(LocalDate.of(2028, 2, 29), leapDomain.expirationDate)

        val isoDto = ClinicalMaterialDto(
            id = "mat-iso",
            name = "ISO Material",
            invimaRegistration = "INV-ISO",
            batchLotNumber = "LOT-ISO",
            manufacturingDate = "2024-06-15T08:30:00.000Z",
            expirationDate = "2027-06-15T18:00:00+05:00"
        )
        val isoDomain = isoDto.toDomain()
        assertEquals(LocalDate.of(2024, 6, 15), isoDomain.manufacturingDate)
        assertEquals(LocalDate.of(2027, 6, 15), isoDomain.expirationDate)

        val spaceDto = ClinicalMaterialDto(
            id = "mat-space",
            name = "Space Material",
            invimaRegistration = "INV-SPACE",
            batchLotNumber = "LOT-SPACE",
            manufacturingDate = "2023-11-20 14:00:00",
            expirationDate = "2026-11-20 23:59:59"
        )
        val spaceDomain = spaceDto.toDomain()
        assertEquals(LocalDate.of(2023, 11, 20), spaceDomain.manufacturingDate)
        assertEquals(LocalDate.of(2026, 11, 20), spaceDomain.expirationDate)
    }

    @Test
    fun `ClinicalMaterialDto toDomain handles corrupt createdAt and updatedAt timestamps`() {
        val testCases = listOf(
            null to null,
            "" to null,
            "   " to null,
            "invalid-instant" to null,
            "2024-05-10" to null,
            "2024-05-10T10:00:00" to null,
            "2024-05-10T10:00:00Z" to Instant.parse("2024-05-10T10:00:00Z"),
            "2024-05-10T05:00:00-05:00" to Instant.parse("2024-05-10T10:00:00Z")
        )

        for ((inputStr, expectedInstant) in testCases) {
            val dto = ClinicalMaterialDto(
                id = "mat-ts",
                name = "TS Material",
                invimaRegistration = "INV-TS",
                batchLotNumber = "LOT-TS",
                manufacturingDate = "2024-01-01",
                expirationDate = "2026-01-01",
                createdAt = inputStr,
                updatedAt = inputStr
            )

            val domain = dto.toDomain()
            assertEquals("createdAt mapping for '$inputStr'", expectedInstant, domain.createdAt)
            assertEquals("updatedAt mapping for '$inputStr'", expectedInstant, domain.updatedAt)
        }
    }

    // =========================================================================
    // ADVERSARIAL STRESS TESTS: RETRIEVAL & EMPTY MATERIAL NAME PROTECTION
    // =========================================================================

    @Test
    fun `EvolutionMaterialUsedDto toDomain guarantees materialName is NEVER empty or blank upon retrieval`() {
        val blankNames = listOf(null, "", "   ", "\t\n\r", " ")

        for (blankName in blankNames) {
            val dto = EvolutionMaterialUsedDto(
                id = "emu-edge",
                evolutionId = "evo-100",
                materialId = "mat-100",
                materialNameSnapshot = blankName,
                quantityUsed = 1.0,
                lotNumberSnapshot = "LOT-EDGE",
                invimaSnapshot = "INV-EDGE",
                expirationDateSnapshot = "2026-12-31",
                manufacturingDateSnapshot = null
            )

            val domain = dto.toDomain()
            assertFalse("materialName must not be empty", domain.materialName.isEmpty())
            assertFalse("materialName must not be blank", domain.materialName.isBlank())
            assertEquals("Material Clínico", domain.materialName)
            assertEquals("Material Clínico", domain.materialNameSnapshot)
            assertEquals("", domain.manufacturingDateSnapshot)
        }
    }

    // =========================================================================
    // ADVERSARIAL STRESS TESTS: SNAPSHOT PRESERVATION & CATALOG ISOLATION
    // =========================================================================

    @Test
    fun `snapshot preservation - mutating or deactivating catalog material does NOT alter existing snapshot`() = runTest {
        val clinicClock: ClinicClock = mockk()
        val fixedNow = LocalDateTime.of(2026, 10, 30, 9, 30)
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns fixedNow
        val useCase = BuildEvolutionMaterialUsageUseCase(clinicClock)

        val initialMaterial = ClinicalMaterial(
            id = MaterialId("mat-original-001"),
            doctorId = DoctorId("doc-alpha"),
            name = "Sellante Helioseal F",
            invimaRegistration = "INVIMA-2020-001",
            batchLotNumber = "LOTE-HELI-A",
            manufacturingDate = LocalDate.of(2024, 1, 10),
            expirationDate = LocalDate.of(2027, 1, 10),
            isActive = true
        )

        val buildResult = useCase(
            material = initialMaterial,
            quantityUsed = 1.5,
            careDate = fixedNow.toLocalDate()
        )
        assertTrue("Build snapshot should succeed", buildResult.isRight())
        val capturedSnapshot = buildResult.getOrNull()!!

        // Baseline verification
        assertEquals("mat-original-001", capturedSnapshot.materialId)
        assertEquals("Sellante Helioseal F", capturedSnapshot.materialName)
        assertEquals("Sellante Helioseal F", capturedSnapshot.materialNameSnapshot)
        assertEquals("INVIMA-2020-001", capturedSnapshot.invimaSnapshot)
        assertEquals("LOTE-HELI-A", capturedSnapshot.lotNumberSnapshot)
        assertEquals("2024-01-10", capturedSnapshot.manufacturingDateSnapshot)
        assertEquals("2027-01-10", capturedSnapshot.expirationDateSnapshot)
        assertEquals(1.5, capturedSnapshot.quantityUsed, 0.001)

        // Radical catalog mutation
        val mutatedCatalog = initialMaterial.copy(
            name = "Sellante Helioseal Clear (Fórmula Modificada)",
            invimaRegistration = "INVIMA-2025-999-REVISED",
            batchLotNumber = "LOTE-HELI-B-2025",
            manufacturingDate = LocalDate.of(2025, 5, 20),
            expirationDate = LocalDate.of(2028, 5, 20)
        )

        // Verify snapshot was NOT mutated
        assertEquals("mat-original-001", capturedSnapshot.materialId)
        assertEquals("Sellante Helioseal F", capturedSnapshot.materialName)
        assertEquals("Sellante Helioseal F", capturedSnapshot.materialNameSnapshot)
        assertEquals("INVIMA-2020-001", capturedSnapshot.invimaSnapshot)
        assertEquals("LOTE-HELI-A", capturedSnapshot.lotNumberSnapshot)
        assertEquals("2024-01-10", capturedSnapshot.manufacturingDateSnapshot)
        assertEquals("2027-01-10", capturedSnapshot.expirationDateSnapshot)

        // Deactivation in catalog
        val deactivatedCatalog = mutatedCatalog.copy(isActive = false)

        // Snapshot remains untouched
        assertEquals("Sellante Helioseal F", capturedSnapshot.materialName)
        assertEquals("LOTE-HELI-A", capturedSnapshot.lotNumberSnapshot)

        // Subsequent usage of deactivated material must be blocked
        val blockedResult = useCase(
            material = deactivatedCatalog,
            quantityUsed = 1.0,
            careDate = fixedNow.toLocalDate()
        )
        assertTrue("Subsequent usage of deactivated material must fail", blockedResult.isLeft())
        assertEquals(MaterialError.MaterialInactive, blockedResult.leftOrNull())
    }

    @Test
    fun `snapshot preservation - independent evolutions preserve isolated snapshots of the same material ID`() = runTest {
        val clinicClock: ClinicClock = mockk()
        val fixedNow = LocalDateTime.of(2026, 10, 30, 9, 30)
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns fixedNow
        val useCase = BuildEvolutionMaterialUsageUseCase(clinicClock)

        val materialId = MaterialId("mat-anesthetic-99")

        val batch1 = ClinicalMaterial(
            id = materialId,
            doctorId = DoctorId("doc-1"),
            name = "Mepivacaína 3% sin vasoconstrictor",
            invimaRegistration = "INV-MEPI-2024",
            batchLotNumber = "LOTE-MEPI-BATCH-1",
            manufacturingDate = LocalDate.of(2024, 1, 15),
            expirationDate = LocalDate.of(2026, 1, 15),
            isActive = true
        )
        val usage1 = useCase(batch1, quantityUsed = 1.0, careDate = LocalDate.of(2024, 6, 1)).getOrNull()!!

        val batch2 = batch1.copy(
            batchLotNumber = "LOTE-MEPI-BATCH-2",
            manufacturingDate = LocalDate.of(2025, 2, 1),
            expirationDate = LocalDate.of(2027, 2, 1)
        )
        val usage2 = useCase(batch2, quantityUsed = 2.0, careDate = LocalDate.of(2025, 8, 15)).getOrNull()!!

        assertEquals("mat-anesthetic-99", usage1.materialId)
        assertEquals("LOTE-MEPI-BATCH-1", usage1.lotNumberSnapshot)
        assertEquals("2024-01-15", usage1.manufacturingDateSnapshot)

        assertEquals("mat-anesthetic-99", usage2.materialId)
        assertEquals("LOTE-MEPI-BATCH-2", usage2.lotNumberSnapshot)
        assertEquals("2025-02-01", usage2.manufacturingDateSnapshot)

        assertNotEquals(usage1.lotNumberSnapshot, usage2.lotNumberSnapshot)
        assertNotEquals(usage1.manufacturingDateSnapshot, usage2.manufacturingDateSnapshot)
    }
}

