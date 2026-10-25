package com.example.biodontos.presentation.patients.detail

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import com.example.biodontos.domain.model.billing.TreatmentStatus
import com.example.biodontos.domain.model.history.ActivityType
import com.example.biodontos.domain.model.history.PatientActivity
import com.example.biodontos.domain.repository.AnamnesisRepository
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.ClinicalAuditRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import com.example.biodontos.domain.repository.InformedConsentRepository
import com.example.biodontos.domain.repository.MaterialRepository
import com.example.biodontos.domain.repository.PatientRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatientDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val patientRepository: PatientRepository = mockk(relaxed = true)
    private val clinicalRecordRepository: ClinicalRecordRepository = mockk(relaxed = true)
    private val anamnesisRepository: AnamnesisRepository = mockk(relaxed = true)
    private val informedConsentRepository: InformedConsentRepository = mockk(relaxed = true)
    private val evolutionRepository: EvolutionRepository = mockk(relaxed = true)
    private val billingRepository: BillingRepository = mockk(relaxed = true)
    private val appointmentRepository: AppointmentRepository = mockk(relaxed = true)
    private val materialRepository: MaterialRepository = mockk(relaxed = true)
    private val clinicalAuditRepository: ClinicalAuditRepository = mockk(relaxed = true)

    private val testPatientId = "pat-12345"

    private lateinit var viewModel: PatientDetailViewModel

    @Before
    fun setUp() {
        val testPatient = Patient(
            id = testPatientId,
            dniCedula = "10203040",
            firstName = "Andrés",
            firstLastName = "Castro",
            phone = "3109876543"
        )
        val testRecord = ClinicalRecord(
            id = "rec-54321",
            patientId = testPatientId,
            mainDiagnosis = "Caries dentinaria profunda",
            openedAt = "2026-10-01T09:00:00Z"
        )
        val testAnamnesis = Anamnesis(
            patientId = testPatientId,
            alergias = true,
            alergiasDescripcion = "Aspirina"
        )
        val testConsent = InformedConsent(
            id = "cons-111",
            patientId = testPatientId,
            doctorId = "doc-999",
            treatmentSummary = "Operatoria dental",
            status = "SIGNED",
            signedAt = "2026-10-02T10:00:00Z"
        )
        val testMaterial = ClinicalMaterial(
            id = "mat-1",
            name = "Resina Compuesta 3M",
            invimaRegistration = "INVIMA-12345",
            batchLotNumber = "LOTE-99A",
            manufacturingDate = "2024-01-01",
            expirationDate = "2027-01-01"
        )
        val testEvolution = Evolution(
            id = "evo-1",
            recordId = "rec-54321",
            doctorId = "doc-999",
            date = "2026-10-15",
            procedureTime = "11:00:00",
            description = "Profilaxis inicial"
        )
        val testTreatment = Treatment(
            id = "treat-1",
            patientId = PatientId(testPatientId),
            description = "Rehabilitación corona",
            totalCost = CopAmount(500000L),
            status = "PENDIENTE",
            createdAt = LocalDateTime.of(2026, 9, 10, 14, 0)
        )
        val testPayment = Payment(
            id = "pay-1",
            treatmentId = "treat-1",
            amount = CopAmount(200000L),
            paymentMethod = "EFECTIVO",
            createdAt = LocalDateTime.of(2026, 10, 16, 15, 0)
        )

        coEvery { patientRepository.getPatientById(testPatientId) } returns Result.success(testPatient)
        coEvery { clinicalRecordRepository.getRecordByPatientId(testPatientId) } returns Result.success(testRecord)
        coEvery { clinicalRecordRepository.getRecordsByPatientId(testPatientId) } returns Result.success(listOf(testRecord))
        coEvery { anamnesisRepository.getAnamnesisByPatientId(testPatientId) } returns Result.success(testAnamnesis)
        coEvery { informedConsentRepository.getConsentByPatientId(testPatientId) } returns Result.success(testConsent)
        coEvery { informedConsentRepository.getConsentsByPatientId(testPatientId) } returns Result.success(listOf(testConsent))
        coEvery { evolutionRepository.getEvolutionsByRecordId("rec-54321") } returns Result.success(listOf(testEvolution))
        coEvery { billingRepository.getTreatmentsByPatientId(testPatientId) } returns Result.success(listOf(testTreatment))
        coEvery { billingRepository.getPaymentsByPatientId(testPatientId) } returns Result.success(listOf(testPayment))
        coEvery { appointmentRepository.getAppointmentsByPatientId(testPatientId) } returns Result.success(emptyList())
        coEvery { materialRepository.getMaterials() } returns Result.success(listOf(testMaterial))
        coEvery { clinicalAuditRepository.getAuditTrail(testPatientId, any()) } returns Result.success(emptyList())

        viewModel = PatientDetailViewModel(
            patientId = testPatientId,
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
            informedConsentRepository = informedConsentRepository,
            evolutionRepository = evolutionRepository,
            billingRepository = billingRepository,
            appointmentRepository = appointmentRepository,
            materialRepository = materialRepository,
            clinicalAuditRepository = clinicalAuditRepository
        )
    }

    @Test
    fun `Initial load defaults to HISTORY tab and populates timeline and financial summary`() = runTest {
        val s = viewModel.state.value
        assertFalse(s.isLoading)
        assertEquals(PatientDetailTab.HISTORY, s.selectedTab)
        assertNotNull(s.patient)
        assertEquals("Andrés", s.patient?.firstName)
        assertEquals("rec-54321", s.clinicalRecord?.id)
        assertEquals(500000.0, s.totalBilled, 0.01)
        assertEquals(200000.0, s.totalPaid, 0.01)
        assertEquals(300000.0, s.pendingBalance, 0.01)
        assertEquals(5, s.historyState.allActivities.size)
    }

    @Test
    fun `Month and ActivityType filters update filteredActivities accurately`() = runTest {
        viewModel.selectMonthFilter(YearMonth.of(2026, 9))
        assertEquals(1, viewModel.state.value.historyState.filteredActivities.size)

        viewModel.clearHistoryFilters()
        assertEquals(5, viewModel.state.value.historyState.filteredActivities.size)

        viewModel.toggleActivityTypeFilter(ActivityType.EVOLUTION)
        assertFalse(ActivityType.EVOLUTION in viewModel.state.value.historyState.filter.types)
        assertEquals(4, viewModel.state.value.historyState.filteredActivities.size)
    }

    @Test
    fun `Two-step voiding flow validates reason and calls voidEvolutionUseCase`() = runTest {
        coEvery { evolutionRepository.voidEvolution("evo-1", any()) } returns Result.success(Unit)

        val evoActivity = viewModel.state.value.historyState.allActivities
            .filterIsInstance<PatientActivity.EvolutionActivity>()
            .first()

        viewModel.requestVoidActivity(evoActivity)
        assertEquals(1, viewModel.state.value.historyState.voidConfirmationStep)

        // Short reason stays on step 1
        viewModel.onVoidReasonChange("Corto")
        viewModel.advanceVoidConfirmationStep()
        assertEquals(1, viewModel.state.value.historyState.voidConfirmationStep)

        // Valid reason advances to step 2 and confirms
        viewModel.onVoidReasonChange("Registro duplicado por error de red")
        viewModel.advanceVoidConfirmationStep()
        assertEquals(2, viewModel.state.value.historyState.voidConfirmationStep)

        viewModel.confirmVoidActivity()
        coVerify { evolutionRepository.voidEvolution("evo-1", "Registro duplicado por error de red") }
        assertNull(viewModel.state.value.historyState.voidDialogTarget)
    }

    @Test
    fun `Treatment status update dialog transitions PENDIENTE to APROBADO`() = runTest {
        coEvery {
            billingRepository.updateTreatmentStatus("treat-1", TreatmentStatus.APROBADO, any())
        } returns Result.success(Unit)

        val treatment = viewModel.state.value.treatments.first()
        viewModel.openTreatmentStatusDialog(treatment)
        viewModel.onSelectTargetTreatmentStatus(TreatmentStatus.APROBADO)
        viewModel.confirmTreatmentStatusUpdate()

        coVerify { billingRepository.updateTreatmentStatus("treat-1", TreatmentStatus.APROBADO, any()) }
        assertNull(viewModel.state.value.historyState.statusDialogTarget)
    }

    @Test
    fun `Tab selection updates state`() {
        viewModel.selectTab(PatientDetailTab.EVOLUTIONS)
        assertEquals(PatientDetailTab.EVOLUTIONS, viewModel.state.value.selectedTab)

        viewModel.selectTab(PatientDetailTab.TREATMENTS)
        assertEquals(PatientDetailTab.TREATMENTS, viewModel.state.value.selectedTab)
    }

    @Test
    fun `Saving new evolution adds note with materials and triggers repository`() = runTest {
        coEvery { evolutionRepository.saveEvolution(any()) } returns Result.success(mockk(relaxed = true))

        viewModel.openNewEvolutionDialog()
        assertTrue(viewModel.state.value.showNewEvolutionDialog)

        viewModel.onNewEvolutionDescriptionChange("Restauración oclusal diente 16")
        viewModel.onNewEvolutionToothChange(16)
        viewModel.onNewEvolutionCostChange("120000")
        viewModel.onNewEvolutionPaymentChange("120000")

        val mat = viewModel.state.value.materialsCatalog.first()
        viewModel.onSelectCatalogMaterial(mat)
        viewModel.addMaterialToEvolution(1.0)

        assertEquals(1, viewModel.state.value.newEvolutionMaterials.size)

        viewModel.saveEvolution()

        coVerify { evolutionRepository.saveEvolution(any()) }
        assertFalse(viewModel.state.value.showNewEvolutionDialog)
        assertNotNull(viewModel.state.value.successMessage)
    }

    @Test
    fun `Saving appointment creates appointment for the patient`() = runTest {
        coEvery { appointmentRepository.createAppointment(any()) } returns Result.success(mockk(relaxed = true))

        viewModel.openNewAppointmentDialog()
        assertTrue(viewModel.state.value.showNewAppointmentDialog)

        viewModel.onNewAppointmentDateChange("2026-11-20")
        viewModel.onNewAppointmentTimeChange("14:30")
        viewModel.onNewAppointmentReasonChange("Control post-operatorio")

        viewModel.saveAppointment()

        coVerify { appointmentRepository.createAppointment(match { it.patientId == testPatientId }) }
        assertFalse(viewModel.state.value.showNewAppointmentDialog)
    }
}
