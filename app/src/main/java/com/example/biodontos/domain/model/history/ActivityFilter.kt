package com.example.biodontos.domain.model.history

import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.billing.TreatmentStatus
import java.time.LocalDateTime
import java.time.YearMonth

data class ActivityFilter(
    val month: YearMonth? = null,
    val types: Set<ActivityType> = ActivityType.entries.toSet()
) {
    fun apply(items: List<PatientActivity>): List<PatientActivity> =
        items.filter { item ->
            val matchesType = item.type in types
            val matchesMonth = month == null || (
                item.isDateValid && YearMonth.from(item.occurredAt) == month
            )
            matchesType && matchesMonth
        }
}

data class PatientHistorySummary(
    val totalActivities: Int = 0,
    val lastVisit: LocalDateTime? = null,
    val activeTreatmentsCount: Int = 0,
    val totalBilled: CopAmount = CopAmount.ZERO,
    val totalPaid: CopAmount = CopAmount.ZERO,
    val pendingBalance: CopAmount = CopAmount.ZERO
) {
    companion object {
        fun from(activities: List<PatientActivity>): PatientHistorySummary {
            val lastVisit = activities
                .filterIsInstance<PatientActivity.AppointmentActivity>()
                .filter { it.appointment.status == AppointmentStatus.COMPLETED && it.isDateValid }
                .maxOfOrNull { it.occurredAt }

            val activeStatuses = setOf(
                TreatmentStatus.PENDIENTE,
                TreatmentStatus.APROBADO,
                TreatmentStatus.EN_CURSO
            )

            val budgetActivities = activities.filterIsInstance<PatientActivity.TreatmentBudgetActivity>()
            val activeTreatmentsCount = budgetActivities.count { it.status in activeStatuses }

            val nonCancelledBudgets = budgetActivities.filter { it.status != TreatmentStatus.CANCELADO }
            val totalBilledPesos = nonCancelledBudgets.sumOf { it.treatment.totalCost.pesos }

            val validPayments = activities
                .filterIsInstance<PatientActivity.PaymentActivity>()
                .filter { it.voidInfo == null }
            val totalPaidPesos = validPayments.sumOf { it.payment.amount.pesos }

            val pendingPesos = (totalBilledPesos - totalPaidPesos).coerceAtLeast(0L)

            return PatientHistorySummary(
                totalActivities = activities.size,
                lastVisit = lastVisit,
                activeTreatmentsCount = activeTreatmentsCount,
                totalBilled = CopAmount(totalBilledPesos),
                totalPaid = CopAmount(totalPaidPesos),
                pendingBalance = CopAmount(pendingPesos)
            )
        }
    }
}
