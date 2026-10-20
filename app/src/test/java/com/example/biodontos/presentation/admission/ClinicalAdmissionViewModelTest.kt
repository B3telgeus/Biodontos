package com.example.biodontos.presentation.admission

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.repository.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val guardianRepository: GuardianRepository = mockk(relaxed = true)

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
            appointmentRepository = appointmentRepository,
            guardianRepository = guardianRepository
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
            guardianRepository = guardianRepository,
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

    @Test
    fun `Step 1 saves guardian and associates guardian_id to patient when hasGuardian is true`() = runTest {
        val savedGuardian = Guardian(
            id = "g-uuid-999",
            firstName = "Elena",
            lastName = "Suarez",
            phone = "3151112233",
            address = "Calle 80 # 10-20",
            locality = "Engativá"
        )
        coEvery { guardianRepository.saveGuardian(any()) } returns Result.success(savedGuardian)

        var capturedPatient: Patient? = null
        coEvery { patientRepository.createOrUpdatePatient(any()) } answers {
            capturedPatient = firstArg()
            Result.success(requireNotNull(capturedPatient) { "capturedPatient is null" })
        }
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-1", patientId = "pat-1")
        )

        viewModel.onEvent(ClinicalAdmissionEvent.UpdateDni("1050607080"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Mateo"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gomez"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateAddress("Calle 80 # 10-20"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateLocality("Engativá"))
        viewModel.onEvent(ClinicalAdmissionEvent.ToggleHasGuardian(true))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateGuardianRelationship("Madre"))
        viewModel.onEvent(
            ClinicalAdmissionEvent.UpdateGuardian(
                relationship = "Madre",
                firstName = "Elena",
                lastName = "Suarez",
                phone = "3151112233",
                address = "Calle 80 # 10-20",
                locality = "Engativá"
            )
        )

        // Advance step 1
        viewModel.onEvent(ClinicalAdmissionEvent.NextStep)

        assertEquals(2, viewModel.state.value.currentStep)
        assertNotNull(capturedPatient)
        assertEquals("Engativá", capturedPatient?.locality)
        assertEquals("g-uuid-999", capturedPatient?.guardianId)
        assertEquals("Madre", capturedPatient?.guardianRelationship)
        assertEquals("Elena", capturedPatient?.guardianFirstName)
    }

    @Test
    fun `isMinor is true when patient birthDate indicates less than 18 years`() {
        // Adult born in 1990
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateBirthDate("1990-01-01"))
        assertFalse(viewModel.state.value.isMinor)

        // Minor born in 2015
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateBirthDate("2015-05-20"))
        assertTrue(viewModel.state.value.isMinor)
    }

    @Test
    fun `Operative consent saves informed consent and procedures correctly with valid doctor UUID and preop naming`() = runTest {
        var capturedConsent: InformedConsent? = null
        coEvery { informedConsentRepository.saveConsent(any()) } answers {
            capturedConsent = firstArg()
            Result.success(capturedConsent!!)
        }

        viewModel.onEvent(ClinicalAdmissionEvent.UpdateDni("10102020"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Laura"))
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Rios"))
        viewModel.onEvent(ClinicalAdmissionEvent.ToggleOperativeConsentDialog(true))
        assertTrue(viewModel.state.value.showOperativeConsentDialog)

        viewModel.onEvent(ClinicalAdmissionEvent.UpdateOperativeConsentTreatment("Restauraciones estéticas en resina"))
        viewModel.onEvent(ClinicalAdmissionEvent.AddOperativeConsentProcedure(toothNumber = 26, date = "2026-10-18"))
        viewModel.onEvent(ClinicalAdmissionEvent.ConfirmOperativeConsentSignatures("patient_sig_data", "doctor_sig_data"))

        // Set patientId for saving consent
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-100", dniCedula = "10102020", firstName = "Laura", firstLastName = "Rios")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-100", patientId = "pat-100"))
        viewModel.onEvent(ClinicalAdmissionEvent.NextStep)

        viewModel.onEvent(ClinicalAdmissionEvent.SaveOperativeConsent)

        val state = viewModel.state.value
        assertFalse(state.showOperativeConsentDialog)
        assertTrue(state.operativeConsentSaved)
        assertEquals("Consentimiento de operatoria guardado y firmado correctamente", state.successMessage)

        assertNotNull(capturedConsent)
        // El doctorId debe ser un UUID válido (no "DOC-001")
        assertEquals(ClinicalAdmissionViewModel.DEFAULT_DOCTOR_UUID, capturedConsent?.doctorId)
        // El formato de tratamiento debe incluir el código de documento estandarizado consentimiento_preop_idpaciente_fecha
        assertTrue(capturedConsent?.treatmentSummary?.contains("consentimiento_preop_pat-100_") == true)
        assertTrue(capturedConsent?.treatmentSummary?.contains("Restauraciones estéticas en resina") == true)
        assertEquals("patient_sig_data", capturedConsent?.patientSignatureUrl)
        assertEquals("doctor_sig_data", capturedConsent?.doctorSignatureUrl)
    }

    @Test
    fun `Operative consent uses authenticated doctor UUID when getCurrentDoctorUseCase is provided`() = runTest {
        val doctorUseCase: com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase = mockk()
        val dummyDoctor = Doctor(
            id = DoctorId("99999999-9999-9999-9999-999999999999"),
            userId = "user-uid-999",
            firstName = "Andrés",
            lastName = "Cepeda",
            specialty = "Odontología Restauradora",
            email = "andres.cepeda@biodontos.com",
            licenseNumber = "OD-77889"
        )
        coEvery { doctorUseCase() } returns Result.success(dummyDoctor)

        var capturedConsent: InformedConsent? = null
        coEvery { informedConsentRepository.saveConsent(any()) } answers {
            capturedConsent = firstArg()
            Result.success(capturedConsent!!)
        }

        val vmWithDoctor = ClinicalAdmissionViewModel(
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
            odontogramRepository = odontogramRepository,
            periodontalRepository = periodontalRepository,
            evolutionRepository = evolutionRepository,
            materialRepository = materialRepository,
            informedConsentRepository = informedConsentRepository,
            appointmentRepository = appointmentRepository,
            guardianRepository = guardianRepository,
            getCurrentDoctorUseCase = doctorUseCase
        )

        advanceUntilIdle()

        // Verificamos que se cargó el doctor en el state
        assertEquals("99999999-9999-9999-9999-999999999999", vmWithDoctor.state.value.doctorId)
        assertEquals("Andrés Cepeda", vmWithDoctor.state.value.doctorName)
        assertEquals("OD-77889", vmWithDoctor.state.value.doctorLicense)

        // Simulamos paciente y guardado de consentimiento
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-200", dniCedula = "52889900", firstName = "Camila", firstLastName = "Díaz")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-200", patientId = "pat-200"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.UpdateDni("52889900"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Camila"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Díaz"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        vmWithDoctor.onEvent(ClinicalAdmissionEvent.SaveOperativeConsent)
        advanceUntilIdle()

        assertNotNull(capturedConsent)
        assertEquals("99999999-9999-9999-9999-999999999999", capturedConsent?.doctorId)
        assertEquals("OD-77889", capturedConsent?.doctorLicenseNumber)
        assertTrue(capturedConsent?.treatmentSummary?.contains("consentimiento_preop_pat-200_") == true)
        // Fallbacks automáticos de firma si no se capturaron en el pad
        assertEquals("consentimiento_preop_pat-200_${java.time.LocalDate.now()}_sig_paciente", capturedConsent?.patientSignatureUrl)
        assertEquals("consentimiento_preop_pat-200_${java.time.LocalDate.now()}_sig_doctor", capturedConsent?.doctorSignatureUrl)
    }

    @Test
    fun `Periodontal examination uses existing clinic doctor from doctorRepository when no session exists`() = runTest {
        val doctorRepo: DoctorRepository = mockk()
        val clinicDoctor = Doctor(
            id = DoctorId("55555555-5555-5555-5555-555555555555"),
            userId = "user-doc-55",
            firstName = "Erika",
            lastName = "Bejarano",
            specialty = "Periodoncia",
            email = "erika.b@biodontos.com",
            licenseNumber = "OD-98234-COL"
        )
        coEvery { doctorRepo.getDoctors() } returns Result.success(listOf(clinicDoctor))

        var capturedExam: PeriodontalExamination? = null
        coEvery { periodontalRepository.saveExamination(any()) } answers {
            capturedExam = firstArg()
            Result.success(capturedExam!!)
        }

        val vmWithClinicDoc = ClinicalAdmissionViewModel(
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
            odontogramRepository = odontogramRepository,
            periodontalRepository = periodontalRepository,
            evolutionRepository = evolutionRepository,
            materialRepository = materialRepository,
            informedConsentRepository = informedConsentRepository,
            appointmentRepository = appointmentRepository,
            guardianRepository = guardianRepository,
            doctorRepository = doctorRepo
        )

        advanceUntilIdle()

        // El doctor del estado debe ser el primer doctor de la clínica
        assertEquals("55555555-5555-5555-5555-555555555555", vmWithClinicDoc.state.value.doctorId)
        assertEquals("Erika Bejarano", vmWithClinicDoc.state.value.doctorName)

        // Simulamos avanzar al paso 4 (Periodontograma) y persistir
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-300", patientId = "pat-300"))
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-300", dniCedula = "80991122", firstName = "Daniel", firstLastName = "Cruz")
        )
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-300"))
        coEvery { odontogramRepository.updateToothState(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        vmWithClinicDoc.onEvent(ClinicalAdmissionEvent.UpdateDni("80991122"))
        vmWithClinicDoc.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Daniel"))
        vmWithClinicDoc.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Cruz"))
        // Paso 1 -> 2
        vmWithClinicDoc.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        // Paso 2 -> 3
        vmWithClinicDoc.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        // Paso 3 -> 4
        vmWithClinicDoc.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        // Paso 4 -> 5 (Persiste periodontograma)
        vmWithClinicDoc.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertNotNull(capturedExam)
        assertEquals("55555555-5555-5555-5555-555555555555", capturedExam?.doctorId)
        assertEquals(5, vmWithClinicDoc.state.value.currentStep)
    }
}
