package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.domain.model.Patient
import org.junit.Assert.assertEquals
import org.junit.Test

class PatientMapperTest {

    @Test
    fun patientDtoToDomainMapsCorrectly() {
        val dto = PatientDto(
            id = "123",
            dniCedula = "1010",
            firstName = "John",
            lastName = "Doe",
            hasCriticalAllergies = false
        )

        val domain = dto.toDomain()

        assertEquals("123", domain.id)
        assertEquals("1010", domain.dniCedula)
        assertEquals("John", domain.firstName)
        assertEquals("Doe", domain.lastName)
        assertEquals(false, domain.consentSigned) // Consent default in mapper
        assertEquals(false, domain.hasCriticalAllergies)
    }

    @Test
    fun patientToDtoMapsCorrectly() {
        val domain = Patient(
            id = "123",
            dniCedula = "1010",
            firstName = "John",
            lastName = "Doe",
            consentSigned = true,
            hasCriticalAllergies = false
        )

        val dto = domain.toDto()

        assertEquals("123", dto.id)
        assertEquals("1010", dto.dniCedula)
        assertEquals("John", dto.firstName)
        assertEquals("Doe", dto.lastName)
        assertEquals(false, dto.hasCriticalAllergies)
    }
}
