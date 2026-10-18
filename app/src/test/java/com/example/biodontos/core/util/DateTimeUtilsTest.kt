package com.example.biodontos.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class DateTimeUtilsTest {

    @Test
    fun `parseToClinicalZonedDateTime correctly converts UTC ISO string to America Bogota`() {
        // 13:00 UTC corresponds to 08:00 AM in America/Bogota (UTC-5)
        val utcIso = "2026-10-25T13:00:00Z"
        val zdt = DateTimeUtils.parseToClinicalZonedDateTime(utcIso)

        assertNotNull(zdt)
        assertEquals(8, zdt?.hour)
        assertEquals(0, zdt?.minute)
        assertEquals(25, zdt?.dayOfMonth)
        assertEquals("America/Bogota", zdt?.zone?.id)
    }

    @Test
    fun `parseToClinicalZonedDateTime parses Bogota offset string correctly`() {
        val bogotaIso = "2026-10-25T09:00:00-05:00"
        val zdt = DateTimeUtils.parseToClinicalZonedDateTime(bogotaIso)

        assertNotNull(zdt)
        assertEquals(9, zdt?.hour)
        assertEquals(0, zdt?.minute)
    }

    @Test
    fun `extractTime formats Bogota time accurately`() {
        val utcIso = "2026-10-25T14:00:00Z" // 14:00 UTC = 09:00 AM in Bogota
        val timeStr = DateTimeUtils.extractTime(utcIso)
        assertEquals("09:00 AM", timeStr)

        val afternoonUtc = "2026-10-25T19:00:00Z" // 19:00 UTC = 02:00 PM in Bogota
        val afternoonStr = DateTimeUtils.extractTime(afternoonUtc)
        assertEquals("02:00 PM", afternoonStr)
    }

    @Test
    fun `buildDateTimeString formats ISO string with Bogota offset`() {
        val result = DateTimeUtils.buildDateTimeString("2026-10-25", "08:00 AM")
        assertEquals("2026-10-25T08:00:00-05:00", result)

        val resultPm = DateTimeUtils.buildDateTimeString("2026-10-25", "02:00 PM")
        assertEquals("2026-10-25T14:00:00-05:00", resultPm)
    }

    @Test
    fun `computeAvailableSlots filters occupied slots and passed hours for today`() {
        val allSlots = listOf(
            "08:00 AM",
            "09:00 AM",
            "10:00 AM",
            "11:00 AM",
            "02:00 PM",
            "03:00 PM"
        )
        // Cita tomada a las 11:00 AM (16:00 UTC)
        val taken = listOf("2026-10-25T16:00:00Z")
        val todayStr = DateTimeUtils.getTodayDate()

        // Simular que la hora actual es 09:30 AM
        val simulatedNow = LocalTime.of(9, 30)

        val available = DateTimeUtils.computeAvailableSlots(
            allSlots = allSlots,
            takenStartTimes = taken,
            selectedDate = todayStr,
            referenceTime = simulatedNow
        )

        // 08:00 AM y 09:00 AM deben quedar fuera por haber pasado ya
        assertFalse(available.contains("08:00 AM"))
        assertFalse(available.contains("09:00 AM"))

        // 10:00 AM debe estar disponible
        assertTrue(available.contains("10:00 AM"))

        // 11:00 AM está ocupado por otra cita, debe quedar fuera
        assertFalse(available.contains("11:00 AM"))

        // 02:00 PM y 03:00 PM deben estar disponibles
        assertTrue(available.contains("02:00 PM"))
        assertTrue(available.contains("03:00 PM"))
    }

    @Test
    fun `computeAvailableSlots preserves future date slots even if earlier than current time`() {
        val allSlots = listOf("08:00 AM", "09:00 AM", "10:00 AM")
        val futureDate = LocalDate.now(DateTimeUtils.CLINICAL_ZONE).plusDays(2).toString()
        val simulatedNow = LocalTime.of(15, 0) // 3 PM

        val available = DateTimeUtils.computeAvailableSlots(
            allSlots = allSlots,
            takenStartTimes = emptyList(),
            selectedDate = futureDate,
            referenceTime = simulatedNow
        )

        // En una fecha futura, las horas matutinas deben estar disponibles
        assertEquals(3, available.size)
        assertTrue(available.contains("08:00 AM"))
    }

    @Test
    fun `isUpcoming correctly discriminates past from future appointments`() {
        val past = ZonedDateTime.now(DateTimeUtils.CLINICAL_ZONE).minusHours(2).toString()
        val future = ZonedDateTime.now(DateTimeUtils.CLINICAL_ZONE).plusHours(2).toString()

        assertFalse(DateTimeUtils.isUpcoming(past))
        assertTrue(DateTimeUtils.isUpcoming(future))
    }
}
