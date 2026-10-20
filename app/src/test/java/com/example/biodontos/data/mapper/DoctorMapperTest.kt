package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.domain.model.Doctor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DoctorMapperTest {

    @Test
    fun `toDomain maps DTO fields correctly`() {
        val dto = DoctorDto(
            id = "doc-uuid-1",
            userId = "usr-uuid-1",
            firstName = "Carlos",
            lastName = "Mendoza",
            specialty = "Ortodoncia & Estética",
            email = "carlos.mendoza@biodontos.com",
            phone = "3101234567",
            licenseNumber = "OD-20381"
        )

        val domain = dto.toDomain()

        assertEquals("doc-uuid-1", domain.id)
        assertEquals("usr-uuid-1", domain.userId)
        assertEquals("Carlos", domain.firstName)
        assertEquals("Mendoza", domain.lastName)
        assertEquals("Ortodoncia & Estética", domain.specialty)
        assertEquals("carlos.mendoza@biodontos.com", domain.email)
        assertEquals("3101234567", domain.phone)
        assertEquals("OD-20381", domain.licenseNumber)
    }

    @Test
    fun `toDomain uses userId as fallback when id is null`() {
        val dto = DoctorDto(
            id = null,
            userId = "usr-fallback-1",
            firstName = "Laura",
            lastName = "Gómez",
            specialty = "Odontología General",
            email = "laura@biodontos.com",
            licenseNumber = "OD-10492"
        )

        val domain = dto.toDomain()

        assertEquals("usr-fallback-1", domain.id)
        assertEquals("usr-fallback-1", domain.userId)
    }

    @Test
    fun `toDto maps domain to DTO correctly`() {
        val domain = Doctor(
            id = "doc-uuid-1",
            userId = "usr-uuid-1",
            firstName = "Carlos",
            lastName = "Mendoza",
            specialty = "Ortodoncia & Estética",
            email = "carlos.mendoza@biodontos.com",
            phone = "3101234567",
            licenseNumber = "OD-20381"
        )

        val dto = domain.toDto()

        assertEquals("doc-uuid-1", dto.id)
        assertEquals("usr-uuid-1", dto.userId)
        assertEquals("Carlos", dto.firstName)
        assertEquals("Mendoza", dto.lastName)
        assertEquals("Ortodoncia & Estética", dto.specialty)
        assertEquals("carlos.mendoza@biodontos.com", dto.email)
        assertEquals("3101234567", dto.phone)
        assertEquals("OD-20381", dto.licenseNumber)
    }

    @Test
    fun `toDto maps empty id to null`() {
        val domain = Doctor(
            id = "",
            userId = "usr-uuid-1",
            firstName = "Valentina",
            lastName = "Ríos",
            specialty = "Endodoncia",
            email = "valentina@biodontos.com",
            licenseNumber = "OD-30918"
        )

        val dto = domain.toDto()
        assertNull(dto.id)
    }
}
