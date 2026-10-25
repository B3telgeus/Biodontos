package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.DoctorAvailabilityDto
import com.example.biodontos.data.dto.toDomain
import com.example.biodontos.data.dto.toDto
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class DoctorAvailabilityMapperTest {

    @Test
    fun `DoctorAvailabilityDto toDomain maps fields accurately`() {
        val dto = DoctorAvailabilityDto(
            id = "avail-1",
            doctorId = "doc-1",
            dayOfWeek = 3,
            startTime = "08:30:00",
            endTime = "16:30:00",
            slotMinutes = 45,
            isActive = true
        )

        val domain = dto.toDomain()

        assertEquals("avail-1", domain.id)
        assertEquals("doc-1", domain.doctorId.value)
        assertEquals(DayOfWeek.WEDNESDAY, domain.dayOfWeek)
        assertEquals(LocalTime.of(8, 30), domain.startTime)
        assertEquals(LocalTime.of(16, 30), domain.endTime)
        assertEquals(45, domain.slotMinutes)
        assertEquals(true, domain.isActive)
    }

    @Test
    fun `WeeklyAvailability toDto maps fields accurately`() {
        val domain = WeeklyAvailability(
            id = "",
            doctorId = DoctorId("doc-1"),
            dayOfWeek = DayOfWeek.FRIDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            slotMinutes = 30,
            isActive = false
        )

        val dto = domain.toDto()

        assertNull(dto.id)
        assertEquals("doc-1", dto.doctorId)
        assertEquals(5, dto.dayOfWeek)
        assertEquals("09:00:00", dto.startTime)
        assertEquals("17:00:00", dto.endTime)
        assertEquals(30, dto.slotMinutes)
        assertEquals(false, dto.isActive)
    }
}
