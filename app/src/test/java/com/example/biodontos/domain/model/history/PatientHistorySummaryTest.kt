package com.example.biodontos.domain.model.history

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PatientHistorySummaryTest {

    @Test
    fun `summary excludes voided payments and cancelled treatments from pending balance`() {
        val now = LocalDateTime.of(2026, 10, 20, 11, 0)
        val voidInfo = VoidInfo(voidedAt = now, reason = "Cobro duplicado por error")

        val activities = listOf(
            PatientActivity.TreatmentBudgetActivity(
                id = "tr-active",
                treatment = Treatment(
                    id = "1",
                    patientId = PatientId("pat-1"),
                    description = "Endodoncia birradicular",
                    totalCost = CopAmount(400000L),
                    status = "EN_CURSO"
                ),
                status = TreatmentStatus.EN_CURSO,
                occurredAt = now
            ),
            PatientActivity.TreatmentBudgetActivity(
                id = "tr-cancelled",
                treatment = Treatment(
                    id = "2",
                    patientId = PatientId("pat-1"),
                    description = "Blanqueamiento cancelado",
                    totalCost = CopAmount(300000L),
                    status = "CANCELADO"
                ),
                status = TreatmentStatus.CANCELADO,
                occurredAt = now
            ),
            PatientActivity.PaymentActivity(
                id = "pay-valid",
                payment = Payment(
                    id = "p-1",
                    treatmentId = "1",
                    amount = CopAmount(150000L)
                ),
                treatmentDescription = "Endodoncia birradicular",
                occurredAt = now
            ),
            PatientActivity.PaymentActivity(
                id = "pay-voided",
                payment = Payment(
                    id = "p-2",
                    treatmentId = "1",
                    amount = CopAmount(100000L),
                    voidInfo = voidInfo
                ),
                treatmentDescription = "Endodoncia birradicular",
                voidInfo = voidInfo,
                occurredAt = now
            ),
            PatientActivity.AppointmentActivity(
                id = "appt-completed",
                appointment = Appointment(
                    id = "a-1",
                    patientId = "pat-1",
                    startTime = now.minusDays(3),
                    status = AppointmentStatus.COMPLETED
                ),
                occurredAt = now.minusDays(3)
            )
        )

        val summary = PatientHistorySummary.from(activities)

        assertEquals(5, summary.totalActivities)
        assertEquals(1, summary.activeTreatmentsCount)
        assertEquals(400000L, summary.totalBilled.pesos)
        assertEquals(150000L, summary.totalPaid.pesos)
        assertEquals(250000L, summary.pendingBalance.pesos)
        assertEquals(now.minusDays(3), summary.lastVisit)
    }
}
