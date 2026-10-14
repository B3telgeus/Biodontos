package com.example.biodontos.presentation.admission

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.repository.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClinicalAdmissionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val patientRepository: PatientRepository = mockk(relaxed = true)
    private val clinicalRecordRepository: ClinicalRecordRepository = mockk(relaxed = true)
    private val anamnesisRepository: AnamnesisRepository = mockk(relaxed = true)
    private val odontogramRepository: OdontogramRepository = mockk(relaxed = true)
    private val periodontalRepository: PeriodontalRepository = mockk(relaxed = true)
    private val evolutionRepository: EvolutionRepository = mockk(relaxed = true)
    private val materialRepository: MaterialRepository = mockk(relaxed = true)
    private val informedConsentRepository: InformedConsentRepository = mockk(relaxed = true)
    private val appointmentRepository: AppointmentRepository = mockk(relaxed = true)

    private lateinit var viewModel: ClinicalAdmissionViewModel

    @Before
    fun setUp() {
        coEvery { materialRepository.getMaterials() } returns Result.success(
            listOf(
                ClinicalMaterial(
                    id = "mat-1",
                    name = "Resina Filtek Z350 XT",
                    invimaRegistration = "INVIMA 2018DM-0018241",
                    batchLotNumber = "LOTE-9921A",
                    manufacturingDate = "2024-01-10",
                    expirationDate = "2027-01-10"
                )
            )
        )

        viewModel = ClinicalAdmissionViewModel(
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
            odontogramRepository = odontogramRepository,
            periodontalRepository = periodontalRepository,
            evolutionRepository = evolutionRepository,
            materialRepository = materialRepository,
            informedConsentRepository = informedConsentRepository,
            appointmentRepository = appointmentRepository
        )
    }

    @Test
    fun `Initial state starts at Step 1 and loads materials catalog`() = runTest {
        val state = viewModel.state.value
        assertEquals(1, state.currentStep)
        assertEquals(1, state.materialsCatalog.size)
        assertEquals("Resina Filtek Z350 XT", state.materialsCatalog.first().name)
    }

    @Test
    fun `Advancing step 1 without DNI fails with validation error`() = runTest {
        viewModel.onEvent(ClinicalAdmissionEvent.NextStep)

        val state = viewModel.state.value
        assertEquals(1, state.currentStep)
        assertNotNull(state.dniError)
    }

    @Test
    fun `Advancing step 1 with valid data saves patient and advances to step 2`() = runTest {
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(
                id = "pat-123",
                dniCedula = "1020304050",
                firstName = "Carlos",
                firstLastName = "Gómez",
                secondLastName = "Pérez"
            )
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-456", patientId = "pat-123")
        )

        viewModel.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateSecondLastName("Pérez"))
        viewModel.onEvent(ClinicalAdmissionEvent.NextStep)

        val state = viewModel.state.value
        assertEquals(2, state.currentStep)
        assertEquals("pat-123", state.patientId)
        assertEquals("rec-456", state.clinicalRecordId)
        assertNull(state.dniError)
        assertNull(state.nameError)
    }

    @Test
    fun `Evolution balance is correctly calculated as cost minus payment`() {
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateEvolution(
            description = "Resina compuesta",
            cost = "150000",
            payment = "100000"
        ))

        val state = viewModel.state.value
        assertEquals(50000.0, state.evolutionBalance, 0.01)
    }

    @Test
    fun `Adding and removing INVIMA materials updates used materials list`() {
        val material = ClinicalMaterial(
            id = "mat-1",
            name = "Resina Filtek Z350 XT",
            invimaRegistration = "INVIMA 2018DM-0018241",
            batchLotNumber = "LOTE-9921A",
            manufacturingDate = "2024-01-10",
            expirationDate = "2027-01-10"
        )
        viewModel.onEvent(ClinicalAdmissionEvent.SelectMaterialFromCatalog(material))
        viewModel.onEvent(ClinicalAdmissionEvent.AddMaterialUsed(1.0))

        assertEquals(1, viewModel.state.value.usedMaterials.size)
        assertEquals("INVIMA 2018DM-0018241", viewModel.state.value.usedMaterials.first().invimaSnapshot)

        viewModel.onEvent(ClinicalAdmissionEvent.RemoveMaterialUsed("mat-1"))
        assertTrue(viewModel.state.value.usedMaterials.isEmpty())
    }

    @Test
    fun `Auto detects pediatric dentition if birth date indicates under 12 years old`() {
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateBirthDate("2020-05-10"))
        assertEquals(DentitionType.CHILD, viewModel.state.value.dentitionType)

        viewModel.onEvent(ClinicalAdmissionEvent.UpdateBirthDate("1995-08-20"))
        assertEquals(DentitionType.ADULT, viewModel.state.value.dentitionType)
    }

    @Test
    fun `Persisting anamnesis failure updates error message in state`() = runTest {
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-123", dniCedula = "1020304050", firstName = "Carlos", firstLastName = "Gómez")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-456", patientId = "pat-123")
        )
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.failure(Exception("Database connection error"))

        // Advance to step 2
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        viewModel.onEvent(ClinicalAdmissionEvent.NextStep)
        assertEquals(2, viewModel.state.value.currentStep)

        // Try persisting step 2 (anamnesis)
        viewModel.onEvent(ClinicalAdmissionEvent.NextStep)

        val state = viewModel.state.value
        assertEquals(2, state.currentStep)
        assertEquals("Error en anamnesis: Error de conexión. Verifica tu acceso a internet.", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `Initializing with initialPatientId loads existing patient, record and anamnesis`() = runTest {
        val existingPatient = Patient(
            id = "pat-999",
            dniCedula = "80123456",
            firstName = "Lucía",
            firstLastName = "Mora",
            phone = "3001234567"
        )
        val existingRecord = ClinicalRecord(
            id = "rec-888",
            patientId = "pat-999",
            mainDiagnosis = "Pulpitis irreversible"
        )
        val existingAnamnesis = Anamnesis(
            patientId = "pat-999",
            alergias = true,
            alergiasDescripcion = "Penicilina",
            diabetes = true
        )

        coEvery { patientRepository.getPatientById("pat-999") } returns Result.success(existingPatient)
        coEvery { clinicalRecordRepository.getRecordByPatientId("pat-999") } returns Result.success(existingRecord)
        coEvery { anamnesisRepository.getAnamnesisByPatientId("pat-999") } returns Result.success(existingAnamnesis)
        coEvery { odontogramRepository.getOdontogram("pat-999") } returns Result.success(emptyList())
        coEvery { informedConsentRepository.getConsentByPatientId("pat-999") } returns Result.success(null)
        coEvery { periodontalRepository.getExaminationByRecordId("rec-888") } returns Result.success(null)

        val vm = ClinicalAdmissionViewModel(
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
            odontogramRepository = odontogramRepository,
            periodontalRepository = periodontalRepository,
            evolutionRepository = evolutionRepository,
            materialRepository = materialRepository,
            informedConsentRepository = informedConsentRepository,
            appointmentRepository = appointmentRepository,
            initialPatientId = "pat-999"
        )

        val s = vm.state.value
        assertEquals("pat-999", s.patientId)
        assertEquals("80123456", s.dniCedula)
        assertEquals("Lucía", s.firstName)
        assertEquals("Mora", s.firstLastName)
        assertEquals("rec-888", s.clinicalRecordId)
        assertEquals("Pulpitis irreversible", s.mainDiagnosis)
        assertTrue(s.alergias)
        assertTrue(s.diabetes)
        assertFalse(s.isLoading)
    }
}
