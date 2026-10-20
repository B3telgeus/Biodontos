package com.example.biodontos.presentation.patients.detail

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.AnamnesisRepository
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.BillingRepository
import com.example.biodontos.domain.repository.ClinicalRecordRepository
import com.example.biodontos.domain.repository.EvolutionRepository
import com.example.biodontos.domain.repository.InformedConsentRepository
import com.example.biodontos.domain.repository.MaterialRepository
import com.example.biodontos.domain.repository.PatientRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
            mainDiagnosis = "Caries dentinaria profunda"
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
            status = "SIGNED"
        )
        val testMaterial = ClinicalMaterial(
            id = "mat-1",
            name = "Resina Compuesta 3M",
            invimaRegistration = "INVIMA-12345",
            batchLotNumber = "LOTE-99A",
            manufacturingDate = "2024-01-01",
            expirationDate = "2027-01-01"
        )

        coEvery { patientRepository.getPatientById(testPatientId) } returns Result.success(testPatient)
        coEvery { clinicalRecordRepository.getRecordByPatientId(testPatientId) } returns Result.success(testRecord)
        coEvery { anamnesisRepository.getAnamnesisByPatientId(testPatientId) } returns Result.success(testAnamnesis)
        coEvery { informedConsentRepository.getConsentByPatientId(testPatientId) } returns Result.success(testConsent)
        coEvery { evolutionRepository.getEvolutionsByRecordId("rec-54321") } returns Result.success(emptyList())
        coEvery { billingRepository.getTreatmentsByPatientId(testPatientId) } returns Result.success(emptyList())
        coEvery { appointmentRepository.getAppointmentsByPatientId(testPatientId) } returns Result.success(emptyList())
        coEvery { materialRepository.getMaterials() } returns Result.success(listOf(testMaterial))

        viewModel = PatientDetailViewModel(
            patientId = testPatientId,
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
            informedConsentRepository = informedConsentRepository,
            evolutionRepository = evolutionRepository,
            billingRepository = billingRepository,
            appointmentRepository = appointmentRepository,
            materialRepository = materialRepository
        )
    }

    @Test
    fun `Initial load populates patient, clinical record, anamnesis and consent`() = runTest {
        val s = viewModel.state.value
        assertFalse(s.isLoading)
        assertNotNull(s.patient)
        assertEquals("Andrés", s.patient?.firstName)
        assertEquals("rec-54321", s.clinicalRecord?.id)
        assertEquals("Caries dentinaria profunda", s.clinicalRecord?.mainDiagnosis)
        assertTrue(s.anamnesis?.alergias == true)
        assertEquals("SIGNED", s.consent?.status)
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
