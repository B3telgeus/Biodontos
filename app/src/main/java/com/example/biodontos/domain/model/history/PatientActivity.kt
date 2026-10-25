package com.example.biodontos.domain.model.history

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

enum class ActivityType(val label: String) {
    APPOINTMENT("Citas"),
    PAYMENT("Pagos"),
    EVOLUTION("Evoluciones"),
    TREATMENT_BUDGET("Presupuestos"),
    CONSENT("Consentimientos"),
    RECORD_LIFECYCLE("Historia Clínica")
}

enum class RecordEvent(val label: String) {
    OPENED("Apertura de Historia Clínica"),
    CLOSED("Cierre de Historia Clínica")
}

sealed interface PatientActivity {
    val id: String
    val type: ActivityType
    val occurredAt: LocalDateTime
    val isDateValid: Boolean
    val createdStamp: AuditStamp?
    val auditTrail: List<AuditStamp>

    data class AppointmentActivity(
        override val id: String,
        val appointment: Appointment,
        override val occurredAt: LocalDateTime,
        override val isDateValid: Boolean = true,
        override val createdStamp: AuditStamp? = null,
        override val auditTrail: List<AuditStamp> = emptyList()
    ) : PatientActivity {
        override val type: ActivityType = ActivityType.APPOINTMENT
    }

    data class PaymentActivity(
        override val id: String,
        val payment: Payment,
        val treatmentDescription: String,
        val voidInfo: VoidInfo? = payment.voidInfo,
        override val occurredAt: LocalDateTime,
        override val isDateValid: Boolean = true,
        override val createdStamp: AuditStamp? = null,
        override val auditTrail: List<AuditStamp> = emptyList()
    ) : PatientActivity {
        override val type: ActivityType = ActivityType.PAYMENT
    }

    data class EvolutionActivity(
        override val id: String,
        val evolution: Evolution,
        val voidInfo: VoidInfo? = evolution.voidInfo,
        val amendsEvolutionId: String? = evolution.amendsEvolutionId,
        override val occurredAt: LocalDateTime,
        override val isDateValid: Boolean = true,
        override val createdStamp: AuditStamp? = null,
        override val auditTrail: List<AuditStamp> = emptyList()
    ) : PatientActivity {
        override val type: ActivityType = ActivityType.EVOLUTION
    }

    data class TreatmentBudgetActivity(
        override val id: String,
        val treatment: Treatment,
        val status: TreatmentStatus = TreatmentStatus.fromString(treatment.status),
        override val occurredAt: LocalDateTime,
        override val isDateValid: Boolean = true,
        override val createdStamp: AuditStamp? = null,
        override val auditTrail: List<AuditStamp> = emptyList()
    ) : PatientActivity {
        override val type: ActivityType = ActivityType.TREATMENT_BUDGET
    }

    data class ConsentActivity(
        override val id: String,
        val consent: InformedConsent,
        override val occurredAt: LocalDateTime,
        override val isDateValid: Boolean = true,
        override val createdStamp: AuditStamp? = null,
        override val auditTrail: List<AuditStamp> = emptyList()
    ) : PatientActivity {
        override val type: ActivityType = ActivityType.CONSENT
    }

    data class RecordLifecycleActivity(
        override val id: String,
        val record: ClinicalRecord,
        val event: RecordEvent,
        override val occurredAt: LocalDateTime,
        override val isDateValid: Boolean = true,
        override val createdStamp: AuditStamp? = null,
        override val auditTrail: List<AuditStamp> = emptyList()
    ) : PatientActivity {
        override val type: ActivityType = ActivityType.RECORD_LIFECYCLE
    }
}

object ClinicalDateParser {
    val FALLBACK_DATE_TIME: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0)

    data class ParsedDateTime(
        val dateTime: LocalDateTime,
        val isValid: Boolean
    )

    fun parseDateAndTime(dateRaw: String?, timeRaw: String? = null): ParsedDateTime {
        if (dateRaw.isNullOrBlank()) {
            return ParsedDateTime(FALLBACK_DATE_TIME, isValid = false)
        }
        val trimmedDate = dateRaw.trim()
        return runCatching {
            val date = LocalDate.parse(trimmedDate.take(10))
            val time = if (!timeRaw.isNullOrBlank()) {
                runCatching { LocalTime.parse(timeRaw.trim().take(8)) }.getOrDefault(LocalTime.MIDNIGHT)
            } else {
                LocalTime.MIDNIGHT
            }
            ParsedDateTime(LocalDateTime.of(date, time), isValid = true)
        }.getOrElse {
            parseIsoDateTime(trimmedDate)
        }
    }

    fun parseIsoDateTime(raw: String?): ParsedDateTime {
        if (raw.isNullOrBlank()) {
            return ParsedDateTime(FALLBACK_DATE_TIME, isValid = false)
        }
        val trimmed = raw.trim()
        return runCatching {
            ParsedDateTime(OffsetDateTime.parse(trimmed).toLocalDateTime(), isValid = true)
        }.recoverCatching {
            ParsedDateTime(
                Instant.parse(trimmed).atZone(ZoneOffset.UTC).toLocalDateTime(),
                isValid = true
            )
        }.recoverCatching {
            val normalized = trimmed.replace(" ", "T").take(19)
            ParsedDateTime(LocalDateTime.parse(normalized), isValid = true)
        }.recoverCatching {
            val date = LocalDate.parse(trimmed.take(10))
            ParsedDateTime(date.atStartOfDay(), isValid = true)
        }.getOrElse {
            ParsedDateTime(FALLBACK_DATE_TIME, isValid = false)
        }
    }
}
