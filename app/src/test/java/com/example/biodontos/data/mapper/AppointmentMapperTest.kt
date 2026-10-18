package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.AppointmentDto
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentMapperTest {

    @Test
    fun `toDomain maps DTO fields correctly`() {
        val dto = AppointmentDto(
            id = "apt-123",
            patientId = "pat-456",
            doctorId = "doc-789",
            originEvolutionId = "evo-1",
            startTime = "2026-10-25T08:00:00-05:00",
            endTime = "2026-10-25T09:00:00-05:00",
            status = "SCHEDULED",
            isConfirmed = true,
            reason = "Revisión semestral"
        )

        val domain = dto.toDomain()

        assertEquals("apt-123", domain.id)
        assertEquals("pat-456", domain.patientId)
        assertEquals("doc-789", domain.doctorId)
        assertEquals("evo-1", domain.originEvolutionId)
        assertEquals("2026-10-25T08:00:00-05:00", domain.startTime)
        assertEquals("2026-10-25T09:00:00-05:00", domain.endTime)
        assertEquals(AppointmentStatus.SCHEDULED, domain.status)
        assertTrue(domain.isConfirmed)
        assertEquals("Revisión semestral", domain.reason)
    }

    @Test
    fun `toDomain handles null id and unknown status safely`() {
        val dto = AppointmentDto(
            id = null,
            patientId = "pat-456",
            doctorId = null,
            startTime = "2026-10-25T08:00:00-05:00",
            status = "UNKNOWN_CUSTOM_STATUS",
            isConfirmed = false
        )

        val domain = dto.toDomain()

        assertEquals("", domain.id)
        assertEquals(AppointmentStatus.SCHEDULED, domain.status)
        assertFalse(domain.isConfirmed)
        assertNull(domain.doctorId)
    }

    @Test
    fun `toDto maps Domain fields to DTO correctly`() {
        val domain = Appointment(
            id = "apt-123",
            patientId = "pat-456",
            doctorId = "doc-789",
            originEvolutionId = "evo-1",
            startTime = "2026-10-25T08:00:00-05:00",
            endTime = null,
            status = AppointmentStatus.CANCELLED,
            isConfirmed = false,
            reason = "Cancelada por fuerza mayor"
        )

        val dto = domain.toDto()

        assertEquals("apt-123", dto.id)
        assertEquals("pat-456", dto.patientId)
        assertEquals("doc-789", dto.doctorId)
        assertEquals("evo-1", dto.originEvolutionId)
        assertEquals("2026-10-25T08:00:00-05:00", dto.startTime)
        assertNull(dto.endTime)
        assertEquals("CANCELLED", dto.status)
        assertFalse(dto.isConfirmed)
        assertEquals("Cancelada por fuerza mayor", dto.reason)
    }

    @Test
    fun `toDto maps empty id to null for database insertion`() {
        val domain = Appointment(
            id = "",
            patientId = "pat-456",
            doctorId = "doc-789",
            startTime = "2026-10-25T08:00:00-05:00",
            status = AppointmentStatus.SCHEDULED,
            isConfirmed = false
        )

        val dto = domain.toDto()
        assertNull(dto.id)
    }
}
