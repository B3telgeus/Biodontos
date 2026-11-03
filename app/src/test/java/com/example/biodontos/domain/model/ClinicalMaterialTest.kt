package com.example.biodontos.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalMaterialTest {

    @Test(expected = IllegalArgumentException::class)
    fun `MaterialId with blank string throws IllegalArgumentException`() {
        MaterialId("   ")
    }

    @Test
    fun `MaterialId generate produces valid non-blank id`() {
        val id = MaterialId.generate()
        assertTrue(id.value.isNotBlank())
    }

    @Test
    fun `Secondary constructor parses ISO string dates correctly`() {
        val material = ClinicalMaterial(
            id = "mat-123",
            doctorId = "doc-999",
            name = "Resina Fluida",
            invimaRegistration = "INV-2024",
            batchLotNumber = "L-01",
            manufacturingDate = "2024-01-15",
            expirationDate = "2026-01-15",
            isActive = true
        )

        assertEquals("mat-123", material.id.value)
        assertEquals("doc-999", material.doctorId?.value)
        assertEquals(LocalDate.of(2024, 1, 15), material.manufacturingDate)
        assertEquals(LocalDate.of(2026, 1, 15), material.expirationDate)
        assertTrue(material.isActive)
        assertEquals("mat-123", material.rawId)
        assertEquals("2024-01-15", material.rawManufacturingDate)
        assertEquals("2026-01-15", material.rawExpirationDate)
    }

    @Test
    fun `isExpired returns true only when referenceDate is strictly after expirationDate`() {
        val refDate = LocalDate.of(2026, 10, 30)

        val expiredMaterial = ClinicalMaterial(
            name = "Material 1",
            invimaRegistration = "INV-1",
            batchLotNumber = "L-1",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = LocalDate.of(2026, 10, 29)
        )
        assertTrue(expiredMaterial.isExpired(refDate))

        val expiringToday = ClinicalMaterial(
            name = "Material 2",
            invimaRegistration = "INV-2",
            batchLotNumber = "L-2",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = LocalDate.of(2026, 10, 30)
        )
        assertFalse(expiringToday.isExpired(refDate))

        val validMaterial = ClinicalMaterial(
            name = "Material 3",
            invimaRegistration = "INV-3",
            batchLotNumber = "L-3",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = LocalDate.of(2026, 10, 31)
        )
        assertFalse(validMaterial.isExpired(refDate))
    }

    @Test
    fun `isNearExpiration returns true when expiring within 30 days and not expired`() {
        val refDate = LocalDate.of(2026, 10, 30)

        // 10 days until expiration
        val in10Days = ClinicalMaterial(
            name = "M1",
            invimaRegistration = "I1",
            batchLotNumber = "L1",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = refDate.plusDays(10)
        )
        assertTrue(in10Days.isNearExpiration(refDate, thresholdDays = 30))

        // Exactly 30 days until expiration
        val in30Days = ClinicalMaterial(
            name = "M2",
            invimaRegistration = "I2",
            batchLotNumber = "L2",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = refDate.plusDays(30)
        )
        assertTrue(in30Days.isNearExpiration(refDate, thresholdDays = 30))

        // 31 days until expiration (not near expiration)
        val in31Days = ClinicalMaterial(
            name = "M3",
            invimaRegistration = "I3",
            batchLotNumber = "L3",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = refDate.plusDays(31)
        )
        assertFalse(in31Days.isNearExpiration(refDate, thresholdDays = 30))

        // Already expired (isNearExpiration must be false for expired)
        val expired = ClinicalMaterial(
            name = "M4",
            invimaRegistration = "I4",
            batchLotNumber = "L4",
            manufacturingDate = LocalDate.of(2024, 1, 1),
            expirationDate = refDate.minusDays(1)
        )
        assertFalse(expired.isNearExpiration(refDate, thresholdDays = 30))
    }
}
