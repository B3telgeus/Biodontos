package com.example.biodontos.domain.usecase.history

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.ClinicalRecordStatus
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.AuditAction
import com.example.biodontos.domain.model.history.AuditEntry
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.ClinicalAuditRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import com.example.biodontos.domain.repository.InformedConsentRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetPatientActivityTimelineUseCaseTest {

    private val appointmentRepository: AppointmentRepository = mockk()
    private val billingRepository: BillingRepository = mockk()
    private val evolutionRepository: EvolutionRepository = mockk()
    private val clinicalRecordRepository: ClinicalRecordRepository = mockk()
    private val informedConsentRepository: InformedConsentRepository = mockk()
    private val clinicalAuditRepository: ClinicalAuditRepository = mockk()

    private lateinit var useCase: GetPatientActivityTimelineUseCase
    private val patientId = "pat-100"

    @Before
    fun setUp() {
        useCase = GetPatientActivityTimelineUseCase(
            appointmentRepository = appointmentRepository,
            billingRepository = billingRepository,
            evolutionRepository = evolutionRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            informedConsentRepository = informedConsentRepository,
            clinicalAuditRepository = clinicalAuditRepository
        )
    }

    @Test
    fun `aggregates all 6 clinical sources, attaches audit stamps and sorts descending`() = runTest {
        val baseTime = LocalDateTime.of(2026, 10, 10, 10, 0)

        coEvery { appointmentRepository.getAppointmentsByPatientId(patientId) } returns Result.success(
            listOf(
                Appointment(
                    id = "appt-1",
                    patientId = patientId,
                    startTime = baseTime.plusDays(5),
                    status = AppointmentStatus.COMPLETED
                )
            )
        )
        coEvery { billingRepository.getTreatmentsByPatientId(patientId) } returns Result.success(
            listOf(
                Treatment(
                    id = "tr-1",
                    patientId = PatientId(patientId),
                    description = "Corona metal-porcelana",
                    totalCost = CopAmount(600000L),
                    status = "APROBADO",
                    createdAt = baseTime.plusDays(1)
                )
            )
        )
        coEvery { billingRepository.getPaymentsByPatientId(patientId) } returns Result.success(
            listOf(
                Payment(
                    id = "pay-1",
                    treatmentId = "tr-1",
                    amount = CopAmount(200000L),
                    createdAt = baseTime.plusDays(2)
                )
            )
        )
        coEvery { clinicalRecordRepository.getRecordsByPatientId(patientId) } returns Result.success(
            listOf(
                ClinicalRecord(
                    id = "rec-1",
                    patientId = patientId,
                    status = ClinicalRecordStatus.CERRADA,
                    openedAt = "2026-10-01T08:00:00Z",
                    closedAt = "2026-10-20T18:00:00Z"
                )
            )
        )
        coEvery { evolutionRepository.getEvolutionsByRecordId("rec-1") } returns Result.success(
            listOf(
                Evolution(
                    id = "evo-1",
                    recordId = "rec-1",
                    doctorId = "doc-1",
                    date = "2026-10-12",
                    procedureTime = "11:00:00",
                    description = "Tallado dental"
                )
            )
        )
        coEvery { informedConsentRepository.getConsentsByPatientId(patientId) } returns Result.success(
            listOf(
                InformedConsent(
                    id = "cons-1",
                    patientId = patientId,
                    doctorId = "doc-1",
                    treatmentSummary = "Rehabilitación fija",
                    signedAt = "2026-10-02T09:00:00Z",
                    status = "SIGNED"
                )
            )
        )
        coEvery { clinicalAuditRepository.getAuditTrail(patientId, any()) } returns Result.success(
            listOf(
                AuditEntry(
                    id = "aud-1",
                    patientId = patientId,
                    entityTable = "evolutions",
                    entityId = "evo-1",
                    action = AuditAction.CREATE,
                    actorName = "Dr. Santiago Pérez",
                    occurredAt = LocalDateTime.of(2026, 10, 12, 11, 5)
                )
            )
        )

        val result = useCase(patientId)
        assertTrue(result.isSuccess)
        val timeline = result.getOrThrow()

        // 1 appointment + 1 treatment + 1 payment + 1 evolution + 1 consent + 2 record events (open & close) = 7
        assertEquals(7, timeline.activities.size)
        assertTrue(timeline.failedSources.isEmpty())
        assertEquals("rec-close-rec-1", timeline.activities.first().id)

        val evoActivity = timeline.activities.first { it.id == "evo-evo-1" }
        assertEquals("Dr. Santiago Pérez", evoActivity.createdStamp?.actorName)
        assertEquals(400000L, timeline.summary.pendingBalance.pesos)
    }

    @Test
    fun `partial failure records failed source and still returns remaining activities`() = runTest {
        val baseTime = LocalDateTime.of(2026, 10, 10, 10, 0)
        coEvery { appointmentRepository.getAppointmentsByPatientId(patientId) } returns Result.failure(
            RuntimeException("Timeout en citas")
        )
        coEvery { billingRepository.getTreatmentsByPatientId(patientId) } returns Result.success(
            listOf(
                Treatment(
                    id = "tr-1",
                    patientId = PatientId(patientId),
                    description = "Limpieza",
                    totalCost = CopAmount(100000L),
                    createdAt = baseTime
                )
            )
        )
        coEvery { billingRepository.getPaymentsByPatientId(patientId) } returns Result.success(emptyList())
        coEvery { clinicalRecordRepository.getRecordsByPatientId(patientId) } returns Result.success(emptyList())
        coEvery { informedConsentRepository.getConsentsByPatientId(patientId) } returns Result.success(emptyList())
        coEvery { clinicalAuditRepository.getAuditTrail(patientId, any()) } returns Result.success(emptyList())

        val result = useCase(patientId)
        assertTrue(result.isSuccess)
        val timeline = result.getOrThrow()

        assertEquals(1, timeline.activities.size)
        assertTrue(ActivityType.APPOINTMENT in timeline.failedSources)
    }
}
