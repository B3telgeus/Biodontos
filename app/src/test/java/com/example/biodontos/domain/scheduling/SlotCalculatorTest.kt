package com.example.biodontos.domain.scheduling

import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SlotCalculatorTest {

    private val doctorId = DoctorId("doc-123")
    private val testDate = LocalDate.of(2026, 10, 19) // Lunes

    @Test
    fun `calculateAvailableSlots generates slots within morning availability`() {
        val availability = listOf(
            WeeklyAvailability(
                doctorId = doctorId,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(10, 0),
                slotMinutes = 30
            )
        )

        val currentTime = LocalDateTime.of(2026, 10, 18, 12, 0) // El día anterior
        val busyIntervals = emptyList<ClosedRange<LocalDateTime>>()

        val slots = SlotCalculator.calculateAvailableSlots(
            targetDate = testDate,
            availabilities = availability,
            busyIntervals = busyIntervals,
            currentTime = currentTime,
            minLeadTimeHours = 2
        )

        // 8:00-8:30, 8:30-9:00, 9:00-9:30, 9:30-10:00 -> 4 slots
        assertEquals(4, slots.size)
        assertEquals(LocalDateTime.of(2026, 10, 19, 8, 0), slots[0].start)
        assertEquals(LocalDateTime.of(2026, 10, 19, 8, 30), slots[0].end)
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 30), slots[3].start)
    }

    @Test
    fun `calculateAvailableSlots excludes busy collision intervals`() {
        val availability = listOf(
            WeeklyAvailability(
                doctorId = doctorId,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(10, 0),
                slotMinutes = 30
            )
        )

        val currentTime = LocalDateTime.of(2026, 10, 18, 12, 0)
        // Cita ocupada de 8:30 a 9:00
        val busyIntervals = listOf(
            LocalDateTime.of(2026, 10, 19, 8, 30)..LocalDateTime.of(2026, 10, 19, 9, 0)
        )

        val slots = SlotCalculator.calculateAvailableSlots(
            targetDate = testDate,
            availabilities = availability,
            busyIntervals = busyIntervals,
            currentTime = currentTime,
            minLeadTimeHours = 2
        )

        assertEquals(3, slots.size)
        // No debe estar el slot de 8:30
        assertTrue(slots.none { it.start == LocalDateTime.of(2026, 10, 19, 8, 30) })
    }

    @Test
    fun `calculateAvailableSlots filters out slots within min lead time`() {
        val availability = listOf(
            WeeklyAvailability(
                doctorId = doctorId,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(12, 0),
                slotMinutes = 30
            )
        )

        // Hora actual: Lunes 8:15 AM -> Con 2 horas mínimas de anticipación, solo a partir de 10:15 AM
        val currentTime = LocalDateTime.of(2026, 10, 19, 8, 15)

        val slots = SlotCalculator.calculateAvailableSlots(
            targetDate = testDate,
            availabilities = availability,
            busyIntervals = emptyList(),
            currentTime = currentTime,
            minLeadTimeHours = 2
        )

        assertTrue(slots.all { it.start >= LocalDateTime.of(2026, 10, 19, 10, 15) })
        assertEquals(LocalDateTime.of(2026, 10, 19, 10, 30), slots.first().start)
    }
}
