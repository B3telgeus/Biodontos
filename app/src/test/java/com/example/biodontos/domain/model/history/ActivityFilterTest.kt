package com.example.biodontos.domain.model.history

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityFilterTest {

    private val oct2026 = LocalDateTime.of(2026, 10, 15, 10, 30)
    private val sep2026 = LocalDateTime.of(2026, 9, 5, 16, 0)

    private val sampleActivities = listOf(
        PatientActivity.AppointmentActivity(
            id = "appt-1",
            appointment = Appointment(
                id = "1",
                patientId = "pat-1",
                startTime = oct2026,
                status = AppointmentStatus.CONFIRMED
            ),
            occurredAt = oct2026
        ),
        PatientActivity.PaymentActivity(
            id = "pay-1",
            payment = Payment(
                id = "1",
                treatmentId = "tr-1",
                amount = CopAmount(150000L)
            ),
            treatmentDescription = "Profilaxis",
            occurredAt = oct2026.plusHours(1)
        ),
        PatientActivity.EvolutionActivity(
            id = "evo-1",
            evolution = Evolution(
                id = "1",
                recordId = "rec-1",
                doctorId = "doc-1",
                date = "2026-09-05",
                procedureTime = "16:00:00",
                description = "Resina oclusal"
            ),
            occurredAt = sep2026
        ),
        PatientActivity.TreatmentBudgetActivity(
            id = "treat-1",
            treatment = Treatment(
                id = "tr-1",
                patientId = PatientId("pat-1"),
                description = "Plan integral",
                totalCost = CopAmount(500000L)
            ),
            occurredAt = sep2026
        ),
        PatientActivity.ConsentActivity(
            id = "cons-1",
            consent = InformedConsent(
                id = "c-1",
                patientId = "pat-1",
                doctorId = "doc-1",
                treatmentSummary = "Consentimiento general"
            ),
            occurredAt = ClinicalDateParser.FALLBACK_DATE_TIME,
            isDateValid = false
        ),
        PatientActivity.RecordLifecycleActivity(
            id = "rec-1",
            record = ClinicalRecord(id = "r-1", patientId = "pat-1"),
            event = RecordEvent.OPENED,
            occurredAt = sep2026
        )
    )

    @Test
    fun `default filter returns all activities including invalid dates`() {
        val filter = ActivityFilter()
        val result = filter.apply(sampleActivities)
        assertEquals(6, result.size)
    }

    @Test
    fun `filtering by month returns only valid activities matching YearMonth`() {
        val filter = ActivityFilter(month = YearMonth.of(2026, 10))
        val result = filter.apply(sampleActivities)
        assertEquals(2, result.size)
        assertTrue(result.all { YearMonth.from(it.occurredAt) == YearMonth.of(2026, 10) })
    }

    @Test
    fun `filtering by specific activity types returns only selected types`() {
        val filter = ActivityFilter(types = setOf(ActivityType.EVOLUTION, ActivityType.RECORD_LIFECYCLE))
        val result = filter.apply(sampleActivities)
        assertEquals(2, result.size)
        assertTrue(result.any { it.type == ActivityType.EVOLUTION })
        assertTrue(result.any { it.type == ActivityType.RECORD_LIFECYCLE })
    }

    @Test
    fun `combined month and type filter intersects both criteria`() {
        val filter = ActivityFilter(
            month = YearMonth.of(2026, 9),
            types = setOf(ActivityType.EVOLUTION)
        )
        val result = filter.apply(sampleActivities)
        assertEquals(1, result.size)
        assertEquals("evo-1", result.first().id)
    }

    @Test
    fun `ClinicalDateParser tolerates invalid and ISO strings without throwing`() {
        val validDateTime = ClinicalDateParser.parseDateAndTime("2026-10-20", "14:15:00")
        assertTrue(validDateTime.isValid)
        assertEquals(2026, validDateTime.dateTime.year)
        assertEquals(14, validDateTime.dateTime.hour)

        val validIso = ClinicalDateParser.parseIsoDateTime("2026-10-20T18:30:00Z")
        assertTrue(validIso.isValid)

        val invalid = ClinicalDateParser.parseDateAndTime("not-a-date", "bad-time")
        assertFalse(invalid.isValid)
        assertEquals(ClinicalDateParser.FALLBACK_DATE_TIME, invalid.dateTime)
    }
}
