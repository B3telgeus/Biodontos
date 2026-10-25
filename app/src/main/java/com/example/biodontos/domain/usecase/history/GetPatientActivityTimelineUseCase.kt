package com.example.biodontos.domain.usecase.history

import com.example.biodontos.domain.model.ClinicalRecordStatus
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.AuditAction
import com.example.biodontos.domain.model.history.AuditEntry
import com.example.biodontos.domain.model.history.AuditStamp
import com.example.biodontos.domain.model.history.ClinicalDateParser
import com.example.biodontos.domain.model.history.PatientActivity
import com.example.biodontos.domain.model.history.PatientHistorySummary
import com.example.biodontos.domain.model.history.RecordEvent
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.ClinicalAuditRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import com.example.biodontos.domain.repository.InformedConsentRepository
import java.time.YearMonth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class TimelineResult(
    val activities: List<PatientActivity>,
    val availableMonths: List<YearMonth>,
    val summary: PatientHistorySummary,
    val failedSources: Set<ActivityType> = emptySet()
)

class GetPatientActivityTimelineUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val billingRepository: BillingRepository,
    private val evolutionRepository: EvolutionRepository,
    private val clinicalRecordRepository: ClinicalRecordRepository,
    private val informedConsentRepository: InformedConsentRepository,
    private val clinicalAuditRepository: ClinicalAuditRepository
) {
    suspend operator fun invoke(patientId: String): Result<TimelineResult> = coroutineScope {
        val appointmentsDeferred = async { appointmentRepository.getAppointmentsByPatientId(patientId) }
        val treatmentsDeferred = async { billingRepository.getTreatmentsByPatientId(patientId) }
        val paymentsDeferred = async { billingRepository.getPaymentsByPatientId(patientId) }
        val recordsDeferred = async { clinicalRecordRepository.getRecordsByPatientId(patientId) }
        val consentsDeferred = async { informedConsentRepository.getConsentsByPatientId(patientId) }
        val auditDeferred = async { clinicalAuditRepository.getAuditTrail(patientId) }

        val appointmentsRes = appointmentsDeferred.await()
        val treatmentsRes = treatmentsDeferred.await()
        val paymentsRes = paymentsDeferred.await()
        val recordsRes = recordsDeferred.await()
        val consentsRes = consentsDeferred.await()
        val auditRes = auditDeferred.await()

        val failedSources = mutableSetOf<ActivityType>()
        if (appointmentsRes.isFailure) failedSources.add(ActivityType.APPOINTMENT)
        if (treatmentsRes.isFailure) failedSources.add(ActivityType.TREATMENT_BUDGET)
        if (paymentsRes.isFailure) failedSources.add(ActivityType.PAYMENT)
        if (recordsRes.isFailure) failedSources.add(ActivityType.RECORD_LIFECYCLE)
        if (consentsRes.isFailure) failedSources.add(ActivityType.CONSENT)

        val records = recordsRes.getOrDefault(emptyList())
        val evolutionsList = mutableListOf<com.example.biodontos.domain.model.Evolution>()
        for (rec in records) {
            if (rec.id.isNotBlank()) {
                val evoRes = evolutionRepository.getEvolutionsByRecordId(rec.id)
                if (evoRes.isFailure) {
                    failedSources.add(ActivityType.EVOLUTION)
                } else {
                    evolutionsList.addAll(evoRes.getOrDefault(emptyList()))
                }
            }
        }

        // If every single primary clinical source failed, propagate failure
        val allPrimaryFailed = appointmentsRes.isFailure &&
            treatmentsRes.isFailure &&
            paymentsRes.isFailure &&
            recordsRes.isFailure &&
            consentsRes.isFailure
        if (allPrimaryFailed) {
            val firstError = appointmentsRes.exceptionOrNull()
                ?: treatmentsRes.exceptionOrNull()
                ?: Exception("No se pudo obtener la línea de tiempo clínica del paciente.")
            return@coroutineScope Result.failure(firstError)
        }

        val auditByEntity: Map<Pair<String, String>, List<AuditEntry>> =
            auditRes.getOrDefault(emptyList())
                .groupBy { it.entityTable to it.entityId }

        val treatments = treatmentsRes.getOrDefault(emptyList())
        val treatmentDescById = treatments.associate { it.id to it.description }

        val activities = mutableListOf<PatientActivity>()

        // 1. Appointments
        for (appt in appointmentsRes.getOrDefault(emptyList())) {
            val stamps = stampsFor(auditByEntity, "appointments", appt.id)
            val createdStamp = stamps.firstOrNull { it.action == AuditAction.CREATE } ?: stamps.lastOrNull()
            activities.add(
                PatientActivity.AppointmentActivity(
                    id = "appt-${appt.id}",
                    appointment = appt,
                    occurredAt = appt.startTime,
                    isDateValid = true,
                    createdStamp = createdStamp,
                    auditTrail = stamps
                )
            )
        }

        // 2. Treatments / Budgets
        for (treat in treatments) {
            val stamps = stampsFor(auditByEntity, "treatments", treat.id)
            val createdStamp = stamps.firstOrNull { it.action == AuditAction.CREATE } ?: stamps.lastOrNull()
            val occurredAt = treat.createdAt ?: createdStamp?.occurredAt ?: ClinicalDateParser.FALLBACK_DATE_TIME
            val isDateValid = treat.createdAt != null || createdStamp != null
            activities.add(
                PatientActivity.TreatmentBudgetActivity(
                    id = "treat-${treat.id}",
                    treatment = treat,
                    status = TreatmentStatus.fromString(treat.status),
                    occurredAt = occurredAt,
                    isDateValid = isDateValid,
                    createdStamp = createdStamp,
                    auditTrail = stamps
                )
            )
        }

        // 3. Payments
        for (pay in paymentsRes.getOrDefault(emptyList())) {
            val stamps = stampsFor(auditByEntity, "payments", pay.id)
            val createdStamp = stamps.firstOrNull { it.action == AuditAction.CREATE } ?: stamps.lastOrNull()
            val occurredAt = pay.createdAt ?: createdStamp?.occurredAt ?: ClinicalDateParser.FALLBACK_DATE_TIME
            val isDateValid = pay.createdAt != null || createdStamp != null
            activities.add(
                PatientActivity.PaymentActivity(
                    id = "pay-${pay.id}",
                    payment = pay,
                    treatmentDescription = treatmentDescById[pay.treatmentId] ?: "Abono a tratamiento",
                    voidInfo = pay.voidInfo,
                    occurredAt = occurredAt,
                    isDateValid = isDateValid,
                    createdStamp = createdStamp,
                    auditTrail = stamps
                )
            )
        }

        // 4. Evolutions
        for (evo in evolutionsList) {
            val stamps = stampsFor(auditByEntity, "evolutions", evo.id)
            val parsedDate = ClinicalDateParser.parseDateAndTime(evo.date, evo.procedureTime)
            val createdStamp = stamps.firstOrNull { it.action == AuditAction.CREATE }
                ?: evo.doctorName?.takeIf { it.isNotBlank() }?.let { docName ->
                    AuditStamp(
                        actorName = docName,
                        action = AuditAction.CREATE,
                        occurredAt = parsedDate.dateTime
                    )
                }
            activities.add(
                PatientActivity.EvolutionActivity(
                    id = "evo-${evo.id}",
                    evolution = evo,
                    voidInfo = evo.voidInfo,
                    amendsEvolutionId = evo.amendsEvolutionId,
                    occurredAt = parsedDate.dateTime,
                    isDateValid = parsedDate.isValid,
                    createdStamp = createdStamp,
                    auditTrail = stamps
                )
            )
        }

        // 5. Informed Consents
        for (consent in consentsRes.getOrDefault(emptyList())) {
            val stamps = stampsFor(auditByEntity, "informed_consents", consent.id)
            val createdStamp = stamps.firstOrNull { it.action == AuditAction.CREATE } ?: stamps.lastOrNull()
            val parsedDate = ClinicalDateParser.parseIsoDateTime(consent.signedAt)
            val effectiveDate = if (parsedDate.isValid) parsedDate.dateTime
            else createdStamp?.occurredAt ?: ClinicalDateParser.FALLBACK_DATE_TIME
            val isValid = parsedDate.isValid || createdStamp != null
            activities.add(
                PatientActivity.ConsentActivity(
                    id = "consent-${consent.id}",
                    consent = consent,
                    occurredAt = effectiveDate,
                    isDateValid = isValid,
                    createdStamp = createdStamp,
                    auditTrail = stamps
                )
            )
        }

        // 6. Clinical Record Lifecycle (Open & Close)
        for (rec in records) {
            val stamps = stampsFor(auditByEntity, "clinical_records", rec.id)
            val openStamp = stamps.firstOrNull { it.action == AuditAction.CREATE }
            val parsedOpen = ClinicalDateParser.parseIsoDateTime(rec.openedAt)
            val openDate = when {
                parsedOpen.isValid -> parsedOpen.dateTime
                openStamp != null -> openStamp.occurredAt
                else -> ClinicalDateParser.FALLBACK_DATE_TIME
            }
            val isOpenDateValid = parsedOpen.isValid || openStamp != null
            activities.add(
                PatientActivity.RecordLifecycleActivity(
                    id = "rec-open-${rec.id}",
                    record = rec,
                    event = RecordEvent.OPENED,
                    occurredAt = openDate,
                    isDateValid = isOpenDateValid,
                    createdStamp = openStamp,
                    auditTrail = stamps
                )
            )

            if (rec.status == ClinicalRecordStatus.CERRADA || !rec.closedAt.isNullOrBlank()) {
                val closeStamp = stamps.firstOrNull { it.action == AuditAction.STATUS_CHANGE } ?: stamps.firstOrNull()
                val parsedClose = ClinicalDateParser.parseIsoDateTime(rec.closedAt)
                val closeDate = when {
                    parsedClose.isValid -> parsedClose.dateTime
                    closeStamp != null -> closeStamp.occurredAt
                    else -> ClinicalDateParser.FALLBACK_DATE_TIME
                }
                val isCloseDateValid = parsedClose.isValid || closeStamp != null
                activities.add(
                    PatientActivity.RecordLifecycleActivity(
                        id = "rec-close-${rec.id}",
                        record = rec,
                        event = RecordEvent.CLOSED,
                        occurredAt = closeDate,
                        isDateValid = isCloseDateValid,
                        createdStamp = closeStamp,
                        auditTrail = stamps
                    )
                )
            }
        }

        val sortedActivities = activities.sortedWith(
            compareByDescending<PatientActivity> { it.isDateValid }
                .thenByDescending { it.occurredAt }
        )

        val availableMonths = sortedActivities
            .filter { it.isDateValid }
            .map { YearMonth.from(it.occurredAt) }
            .distinct()
            .sortedDescending()

        val summary = PatientHistorySummary.from(sortedActivities)

        Result.success(
            TimelineResult(
                activities = sortedActivities,
                availableMonths = availableMonths,
                summary = summary,
                failedSources = failedSources
            )
        )
    }

    private fun stampsFor(
        auditByEntity: Map<Pair<String, String>, List<AuditEntry>>,
        table: String,
        entityId: String
    ): List<AuditStamp> =
        auditByEntity[table to entityId]
            ?.sortedByDescending { it.occurredAt }
            ?.map { it.toStamp() }
            ?: emptyList()
}
