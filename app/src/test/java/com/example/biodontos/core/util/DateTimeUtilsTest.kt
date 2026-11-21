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
        val utcIso = "2026-10-25T14:00:00Z"
        val timeStr = DateTimeUtils.extractTime(utcIso)
        assertEquals("09:00 AM", timeStr)

        val afternoonUtc = "2026-10-25T19:00:00Z"
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
        val taken = listOf("2026-10-25T16:00:00Z")
        val todayStr = DateTimeUtils.getTodayDate()

        val simulatedNow = LocalTime.of(9, 30)

        val available = DateTimeUtils.computeAvailableSlots(
            allSlots = allSlots,
            takenStartTimes = taken,
            selectedDate = todayStr,
            referenceTime = simulatedNow
        )

        assertFalse(available.contains("08:00 AM"))
        assertFalse(available.contains("09:00 AM"))

        assertTrue(available.contains("10:00 AM"))

        assertFalse(available.contains("11:00 AM"))

        assertTrue(available.contains("02:00 PM"))
        assertTrue(available.contains("03:00 PM"))
    }

    @Test
    fun `computeAvailableSlots preserves future date slots even if earlier than current time`() {
        val allSlots = listOf("08:00 AM", "09:00 AM", "10:00 AM")
        val futureDate = LocalDate.now(DateTimeUtils.CLINICAL_ZONE).plusDays(2).toString()
        val simulatedNow = LocalTime.of(15, 0)

        val available = DateTimeUtils.computeAvailableSlots(
            allSlots = allSlots,
            takenStartTimes = emptyList(),
            selectedDate = futureDate,
            referenceTime = simulatedNow
        )

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
