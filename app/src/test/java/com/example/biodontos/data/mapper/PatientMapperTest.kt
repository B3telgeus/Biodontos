package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.domain.model.Patient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientMapperTest {

    @Test
    fun patientDtoToDomainMapsCorrectlyWithoutAllergies() {
        val dto = PatientDto(
            id = "123",
            dniCedula = "1010",
            firstName = "John",
            lastName = "Doe",
            criticalAllergies = "Ninguna",
            consentSigned = true
        )

        val domain = dto.toDomain()

        assertEquals("123", domain.id)
        assertEquals("1010", domain.dniCedula)
        assertEquals("John", domain.firstName)
        assertEquals("Doe", domain.lastName)
        assertEquals(true, domain.consentSigned)
        assertFalse(domain.hasCriticalAllergies)
        assertEquals("Ninguna", domain.allergiesDescription)
    }

    @Test
    fun patientDtoToDomainMapsCorrectlyWithAllergies() {
        val dto = PatientDto(
            id = "123",
            dniCedula = "1010",
            firstName = "John",
            lastName = "Doe",
            criticalAllergies = "Penicilina, Látex",
            consentSigned = true
        )

        val domain = dto.toDomain()

        assertTrue(domain.hasCriticalAllergies)
        assertEquals("Penicilina, Látex", domain.allergiesDescription)
    }

    @Test
    fun patientToDtoMapsCorrectly() {
        val domain = Patient(
            id = "123",
            dniCedula = "1010",
            firstName = "John",
            lastName = "Doe",
            consentSigned = true,
            hasCriticalAllergies = true,
            allergiesDescription = "Aspirina"
        )

        val dto = domain.toDto()

        assertEquals("123", dto.id)
        assertEquals("1010", dto.dniCedula)
        assertEquals("John", dto.firstName)
        assertEquals("Doe", dto.lastName)
        assertEquals("Aspirina", dto.criticalAllergies)
        assertEquals(true, dto.consentSigned)
    }

    @Test
    fun patientToDtoDefaultsToNingunaWhenNoAllergies() {
        val domain = Patient(
            id = "123",
            dniCedula = "1010",
            firstName = "John",
            lastName = "Doe",
            consentSigned = true,
            hasCriticalAllergies = false
        )

        val dto = domain.toDto()

        assertEquals("Ninguna", dto.criticalAllergies)
    }

    @Test
    fun jsonWithLastNameAndUnknownKeysDeserializesCorrectly() {
        val jsonStr = """{"id":"361","first_name":"Santiago","last_name":"Bejarano","dni_cedula":"102030","extra_col":"bar"}"""
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val dto = json.decodeFromString<PatientDto>(jsonStr)
        val domain = dto.toDomain()

        assertEquals("Santiago", domain.firstName)
        assertEquals("Bejarano", domain.lastName)
    }

    @Test
    fun patientToDtoGeneratesDeterministicUuidFromDniWhenIdIsBlank() {
        val domain = Patient(
            id = "",
            dniCedula = "1098765432",
            firstName = "Carlos",
            lastName = "Perez",
            consentSigned = true
        )

        val dto = domain.toDto()

        val expectedUuid = Patient.generateIdFromDni("1098765432")
        assertEquals(expectedUuid, dto.id)
        assertTrue(dto.id?.isNotEmpty() == true)
        assertEquals(expectedUuid, Patient.generateIdFromDni("1098765432"))
    }
}
