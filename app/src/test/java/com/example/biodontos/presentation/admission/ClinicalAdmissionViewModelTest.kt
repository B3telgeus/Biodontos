package com.example.biodontos.presentation.admission

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.repository.*
import com.example.biodontos.domain.usecase.doctor.GetCurrentDoctorUseCase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

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

    private fun createViewModelWithDoctor(
        doctorId: String = "12345678-1234-1234-1234-123456789abc",
        doctorName: String = "Dr. Carlos Valderrama",
        licenseNumber: String = "OD-12345-COL",
        initialPatientId: String? = null,
        doctorUseCase: GetCurrentDoctorUseCase? = null,
        doctorRepo: DoctorRepository? = null
    ): ClinicalAdmissionViewModel {
        val useCase = doctorUseCase ?: mockk<GetCurrentDoctorUseCase>().also {
            val names = doctorName.removePrefix("Dr. ").removePrefix("Dra. ").trim().split(" ")
            coEvery { it.invoke() } returns Result.success(
                Doctor(
                    id = DoctorId(doctorId),
                    userId = "user-$doctorId",
                    firstName = names.firstOrNull() ?: "Carlos",
                    lastName = names.drop(1).joinToString(" ").ifBlank { "Doctor" },
                    specialty = "Odontología General",
                    licenseNumber = licenseNumber,
                    email = "doctor@biodontos.com"
                )
            )
        }
        return ClinicalAdmissionViewModel(
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
            initialPatientId = initialPatientId,
            getCurrentDoctorUseCase = useCase,
            doctorRepository = doctorRepo
        )
    }

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

    // =========================================================================
    // BASELINE & UI STATE TESTS
    // =========================================================================

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

        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateSecondLastName("Pérez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        val state = vm.state.value
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
    fun `isMinor is true when patient birthDate indicates less than 18 years`() {
        viewModel.onEvent(ClinicalAdmissionEvent.UpdateBirthDate("1990-01-01"))
        assertFalse(viewModel.state.value.isMinor)

        viewModel.onEvent(ClinicalAdmissionEvent.UpdateBirthDate("2015-05-20"))
        assertTrue(viewModel.state.value.isMinor)
    }

    @Test
    fun `Adding expired material in step 6 is rejected and sets errorMessage`() = runTest {
        val expired = ClinicalMaterial(
            id = "mat-exp",
            name = "Anestésico Vencido",
            invimaRegistration = "INVIMA 2016M-000111",
            batchLotNumber = "LOTE-OLD",
            manufacturingDate = "2020-01-10",
            expirationDate = "2022-01-10"
        )
        viewModel.onEvent(ClinicalAdmissionEvent.SelectMaterialFromCatalog(expired))
        viewModel.onEvent(ClinicalAdmissionEvent.AddMaterialUsed(1.0))

        assertTrue(viewModel.state.value.usedMaterials.isEmpty())
        assertEquals(MaterialError.MaterialExpired.userMessage, viewModel.state.value.errorMessage)
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

        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        // Advance to step 2
        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.currentStep)

        // Try persisting step 2 (anamnesis)
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        val state = vm.state.value
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
        advanceUntilIdle()

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

        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1050607080"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Mateo"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gomez"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateAddress("Calle 80 # 10-20"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateLocality("Engativá"))
        vm.onEvent(ClinicalAdmissionEvent.ToggleHasGuardian(true))
        vm.onEvent(ClinicalAdmissionEvent.UpdateGuardianRelationship("Madre"))
        vm.onEvent(
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
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.currentStep)
        assertNotNull(capturedPatient)
        assertEquals("Engativá", capturedPatient?.locality)
        assertEquals("g-uuid-999", capturedPatient?.guardianId)
        assertEquals("Madre", capturedPatient?.guardianRelationship)
        assertEquals("Elena", capturedPatient?.guardianFirstName)
    }

    // =========================================================================
    // REFACTORED TESTS: ELIMINATING OBSOLETE FALLBACKS (TASK REQUIREMENTS a & b)
    // =========================================================================

    /**
     * Requirement (a): Refactor obsolete test asserting DEFAULT_DOCTOR_UUID fallback.
     * When a valid doctor session is present, uses the authenticated doctor's UUID.
     */
    @Test
    fun `Operative consent saves informed consent and procedures correctly with authenticated doctor UUID`() = runTest {
        var capturedConsent: InformedConsent? = null
        coEvery { informedConsentRepository.saveConsent(any()) } answers {
            capturedConsent = firstArg()
            Result.success(requireNotNull(capturedConsent) { "El consentimiento capturado no debe ser nulo" })
        }

        val authDoctorUuid = "44444444-4444-4444-4444-444444444444"
        val vmWithDoctor = createViewModelWithDoctor(doctorId = authDoctorUuid, doctorName = "Dra. Laura Rios")
        advanceUntilIdle()

        vmWithDoctor.onEvent(ClinicalAdmissionEvent.UpdateDni("10102020"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Laura"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Rios"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.ToggleOperativeConsentDialog(true))
        assertTrue(vmWithDoctor.state.value.showOperativeConsentDialog)

        vmWithDoctor.onEvent(ClinicalAdmissionEvent.UpdateOperativeConsentTreatment("Restauraciones estéticas en resina"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.AddOperativeConsentProcedure(toothNumber = 26, date = "2026-10-18"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.ConfirmOperativeConsentSignatures("patient_sig_data", "doctor_sig_data"))

        // Set patientId for saving consent
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-100", dniCedula = "10102020", firstName = "Laura", firstLastName = "Rios")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-100", patientId = "pat-100"))
        vmWithDoctor.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        vmWithDoctor.onEvent(ClinicalAdmissionEvent.SaveOperativeConsent)
        advanceUntilIdle()

        val state = vmWithDoctor.state.value
        assertFalse(state.showOperativeConsentDialog)
        assertTrue(state.operativeConsentSaved)
        assertEquals("Consentimiento de operatoria guardado y firmado correctamente", state.successMessage)

        assertNotNull(capturedConsent)
        // DoctorId must match authenticated doctor's UUID, never DEFAULT_DOCTOR_UUID
        assertEquals(authDoctorUuid, capturedConsent?.doctorId)
        assertTrue(capturedConsent?.treatmentSummary?.contains("consentimiento_preop_pat-100_") == true)
        assertTrue(capturedConsent?.treatmentSummary?.contains("Restauraciones estéticas en resina") == true)
        assertEquals("patient_sig_data", capturedConsent?.patientSignatureUrl)
        assertEquals("doctor_sig_data", capturedConsent?.doctorSignatureUrl)
    }

    /**
     * Requirement (a) companion: When no doctor session is active, saving consent is blocked.
     */
    @Test
    fun `Operative consent without active doctor session blocks save and sets error state`() = runTest {
        var capturedConsent: InformedConsent? = null
        coEvery { informedConsentRepository.saveConsent(any()) } answers {
            capturedConsent = firstArg()
            Result.success(requireNotNull(capturedConsent))
        }

        // ViewModel without doctor session
        val vmWithoutDoctor = ClinicalAdmissionViewModel(
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
            getCurrentDoctorUseCase = null
        )
        advanceUntilIdle()

        // Advance to have patientId
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-100", dniCedula = "10102020", firstName = "Laura", firstLastName = "Rios")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-100", patientId = "pat-100"))
        vmWithoutDoctor.onEvent(ClinicalAdmissionEvent.UpdateDni("10102020"))
        vmWithoutDoctor.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Laura"))
        vmWithoutDoctor.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Rios"))
        vmWithoutDoctor.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        vmWithoutDoctor.onEvent(ClinicalAdmissionEvent.ToggleOperativeConsentDialog(true))
        vmWithoutDoctor.onEvent(ClinicalAdmissionEvent.UpdateOperativeConsentTreatment("Restauraciones estéticas en resina"))
        vmWithoutDoctor.onEvent(ClinicalAdmissionEvent.SaveOperativeConsent)
        advanceUntilIdle()

        val state = vmWithoutDoctor.state.value
        // Strict gate: consent must not be saved when doctor session is missing
        assertNull("Consent must not be saved without an active doctor session", capturedConsent)
        assertFalse("operativeConsentSaved must remain false without active doctor session", state.operativeConsentSaved)
        assertNotNull("Error state must be set when doctor session is missing", state.errorMessage)
    }

    @Test
    fun `Operative consent uses authenticated doctor UUID when getCurrentDoctorUseCase is provided`() = runTest {
        val doctorUseCase: GetCurrentDoctorUseCase = mockk()
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
            Result.success(requireNotNull(capturedConsent) { "El consentimiento capturado no debe ser nulo" })
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

        assertEquals("99999999-9999-9999-9999-999999999999", vmWithDoctor.state.value.doctorId)
        assertEquals("Andrés Cepeda", vmWithDoctor.state.value.doctorName)
        assertEquals("OD-77889", vmWithDoctor.state.value.doctorLicense)

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
        assertEquals("consentimiento_preop_pat-200_${LocalDate.now()}_sig_paciente", capturedConsent?.patientSignatureUrl)
        assertEquals("consentimiento_preop_pat-200_${LocalDate.now()}_sig_doctor", capturedConsent?.doctorSignatureUrl)
    }

    /**
     * Requirement (b): Refactor obsolete test that asserted fallback to clinic doctor in doctorRepository.
     * When NO authenticated doctor session exists, saving periodontal examination is strictly BLOCKED,
     * currentStep does NOT advance to 5, and an error state is set.
     */
    @Test
    fun `Periodontal examination without active doctor session is strictly blocked and does not advance to step 5`() = runTest {
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
            Result.success(requireNotNull(capturedExam) { "El examen periodontal capturado no debe ser nulo" })
        }

        // ViewModel without authenticated doctor session (even though clinic doctor exists in catalog)
        val vmWithoutDoctorSession = ClinicalAdmissionViewModel(
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
            doctorRepository = doctorRepo,
            getCurrentDoctorUseCase = null
        )

        advanceUntilIdle()

        // Setup mock repositories for steps 1-3
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-300", patientId = "pat-300"))
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-300", dniCedula = "80991122", firstName = "Daniel", firstLastName = "Cruz")
        )
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-300"))
        coEvery { odontogramRepository.updateToothState(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        vmWithoutDoctorSession.onEvent(ClinicalAdmissionEvent.UpdateDni("80991122"))
        vmWithoutDoctorSession.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Daniel"))
        vmWithoutDoctorSession.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Cruz"))
        // Paso 1 -> 2
        vmWithoutDoctorSession.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        // Paso 2 -> 3
        vmWithoutDoctorSession.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        // Paso 3 -> 4
        vmWithoutDoctorSession.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        // Paso 4 -> Attempt to persist periodontal examination without doctor session
        vmWithoutDoctorSession.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        // Strictly assert that saving is blocked, step does not advance to 5, and error state is set
        assertNull("Periodontal examination must not be saved without an active doctor session", capturedExam)
        assertNotEquals("Flow must not advance to step 5 without an active doctor session", 5, vmWithoutDoctorSession.state.value.currentStep)
        assertNotNull("Error state must be set when doctor session is missing", vmWithoutDoctorSession.state.value.errorMessage)
    }

    // =========================================================================
    // DOCTOR IDENTITY REQUIREMENT (TIER 1 & TIER 2)
    // =========================================================================

    @Test
    fun `Admission save blocked when getCurrentDoctorUseCase returns null`() = runTest {
        val doctorUseCase: GetCurrentDoctorUseCase = mockk()
        coEvery { doctorUseCase() } returns Result.success(null)

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
            getCurrentDoctorUseCase = doctorUseCase
        )
        advanceUntilIdle()

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
        coVerify(exactly = 0) { patientRepository.createOrUpdatePatient(any()) }
    }

    @Test
    fun `Admission save blocked when getCurrentDoctorUseCase returns failure`() = runTest {
        val doctorUseCase: GetCurrentDoctorUseCase = mockk()
        coEvery { doctorUseCase() } returns Result.failure(Exception("Doctor session expired"))

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
            getCurrentDoctorUseCase = doctorUseCase
        )
        advanceUntilIdle()

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
        coVerify(exactly = 0) { patientRepository.createOrUpdatePatient(any()) }
    }

    @Test
    fun `Emitting error state when doctor session is missing`() = runTest {
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
            getCurrentDoctorUseCase = null
        )
        advanceUntilIdle()

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
    }

    @Test
    fun `Correctly accepting a valid doctor UUID and proceeding with admission`() = runTest {
        val validDoctorUuid = "12345678-abcd-1234-cdef-1234567890ab"
        val vm = createViewModelWithDoctor(doctorId = validDoctorUuid, doctorName = "Dra. Sofia Vergara")
        advanceUntilIdle()

        assertEquals(validDoctorUuid, vm.state.value.doctorId)
        assertEquals("Sofia Vergara", vm.state.value.doctorName)

        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-123", dniCedula = "1020304050", firstName = "Carlos", firstLastName = "Gómez")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-456", patientId = "pat-123")
        )

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.currentStep)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `Malformed or blank doctor ID is rejected from saving admission`() = runTest {
        val doctorUseCase: GetCurrentDoctorUseCase = mockk()
        val legacyDoctor = Doctor(
            id = DoctorId("DOC-001-INVALID"),
            firstName = "Doctor",
            lastName = "Invalido",
            specialty = "General"
        )
        coEvery { doctorUseCase() } returns Result.success(legacyDoctor)

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
            getCurrentDoctorUseCase = doctorUseCase
        )
        advanceUntilIdle()

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
    }

    // =========================================================================
    // STEP ADVANCE GATING (TIER 1 & TIER 2)
    // =========================================================================

    @Test
    fun `Step 1 if patient save fails stays on step 1 and sets error`() = runTest {
        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.failure(Exception("Patient table constraint violation"))

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
        coVerify(exactly = 0) { clinicalRecordRepository.saveRecord(any()) }
    }

    @Test
    fun `Step 1 if record save fails stays on step 1 and sets error`() = runTest {
        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-123", dniCedula = "1020304050", firstName = "Carlos", firstLastName = "Gómez")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.failure(Exception("Record creation error"))

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
    }

    @Test
    fun `Step 3 if tooth state update fails stays on step 3 and sets error`() = runTest {
        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-123", dniCedula = "1020304050", firstName = "Carlos", firstLastName = "Gómez")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-456", patientId = "pat-123")
        )
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-123"))
        coEvery { odontogramRepository.updateToothState(any(), any(), any(), any(), any()) } returns Result.failure(Exception("Odontogram surface persistence failed"))

        // Advance step 1 -> 2
        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.currentStep)

        // Advance step 2 -> 3
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.currentStep)

        // Add surface condition to tooth 11
        vm.onEvent(ClinicalAdmissionEvent.SelectOdontogramTooth(11))
        vm.onEvent(ClinicalAdmissionEvent.SelectConditionToApply(ToothCondition.CARIES))
        vm.onEvent(ClinicalAdmissionEvent.ApplySurfaceCondition(11, ToothSurface.VESTIBULAR))

        // Attempt advance step 3 -> 4
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals("Should stay on step 3 when odontogram persistence fails", 3, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
    }

    @Test
    fun `Step 5 if record save fails stays on step 5 and sets error`() = runTest {
        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        // Setup steps 1-4 success
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-123", dniCedula = "1020304050", firstName = "Carlos", firstLastName = "Gómez")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-456", patientId = "pat-123"))
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-123"))
        coEvery { odontogramRepository.updateToothState(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { periodontalRepository.saveExamination(any()) } returns Result.success(PeriodontalExamination(recordId = "rec-456", doctorId = "12345678-1234-1234-1234-123456789abc", examinationDate = "2026-10-20"))

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 1 -> 2
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 2 -> 3
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 3 -> 4
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 4 -> 5
        advanceUntilIdle()
        assertEquals(5, vm.state.value.currentStep)

        // Step 5: record save fails
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.failure(Exception("Diagnosis update failed"))

        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(5, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
        coVerify(exactly = 0) { informedConsentRepository.saveConsent(any()) }
    }

    @Test
    fun `Step 5 if consent save fails stays on step 5 and sets error`() = runTest {
        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        // Setup steps 1-4 success
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-123", dniCedula = "1020304050", firstName = "Carlos", firstLastName = "Gómez")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-456", patientId = "pat-123"))
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-123"))
        coEvery { odontogramRepository.updateToothState(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { periodontalRepository.saveExamination(any()) } returns Result.success(PeriodontalExamination(recordId = "rec-456", doctorId = "12345678-1234-1234-1234-123456789abc", examinationDate = "2026-10-20"))

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 1 -> 2
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 2 -> 3
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 3 -> 4
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 4 -> 5
        advanceUntilIdle()
        assertEquals(5, vm.state.value.currentStep)

        // Step 5: record save succeeds, consent save fails
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-456", patientId = "pat-123"))
        coEvery { informedConsentRepository.saveConsent(any()) } returns Result.failure(Exception("Informed consent persistence failed"))

        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(5, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
    }

    @Test
    fun `Step 6 if evolution save fails stays on step 6 and does not close record`() = runTest {
        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        // Setup steps 1-5 success
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-123", dniCedula = "1020304050", firstName = "Carlos", firstLastName = "Gómez")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-456", patientId = "pat-123"))
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-123"))
        coEvery { odontogramRepository.updateToothState(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { periodontalRepository.saveExamination(any()) } returns Result.success(PeriodontalExamination(recordId = "rec-456", doctorId = "12345678-1234-1234-1234-123456789abc", examinationDate = "2026-10-20"))
        coEvery { informedConsentRepository.saveConsent(any()) } returns Result.success(InformedConsent(patientId = "pat-123", doctorId = "12345678-1234-1234-1234-123456789abc", treatmentSummary = "Tratamiento Integral de Odontología"))

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 1 -> 2
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 2 -> 3
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 3 -> 4
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 4 -> 5
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 5 -> 6
        advanceUntilIdle()
        assertEquals(6, vm.state.value.currentStep)

        // Step 6: toggle close record, but evolution save fails
        vm.onEvent(ClinicalAdmissionEvent.ToggleCloseRecord(true, "Resumen de cierre médico"))
        coEvery { evolutionRepository.saveEvolution(any()) } returns Result.failure(Exception("RPC network timeout"))

        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(6, vm.state.value.currentStep)
        assertNotNull(vm.state.value.errorMessage)
        coVerify(exactly = 0) { clinicalRecordRepository.closeRecord(any(), any(), any(), any()) }
    }

    @Test
    fun `Step 6 if evolution succeeds advances to step 7 and closes record when requested`() = runTest {
        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        // Setup steps 1-5 success
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-123", dniCedula = "1020304050", firstName = "Carlos", firstLastName = "Gómez")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(ClinicalRecord(id = "rec-456", patientId = "pat-123"))
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-123"))
        coEvery { odontogramRepository.updateToothState(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { periodontalRepository.saveExamination(any()) } returns Result.success(PeriodontalExamination(recordId = "rec-456", doctorId = "12345678-1234-1234-1234-123456789abc", examinationDate = "2026-10-20"))
        coEvery { informedConsentRepository.saveConsent(any()) } returns Result.success(InformedConsent(patientId = "pat-123", doctorId = "12345678-1234-1234-1234-123456789abc", treatmentSummary = "Tratamiento Integral de Odontología"))

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 1 -> 2
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 2 -> 3
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 3 -> 4
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 4 -> 5
        advanceUntilIdle()
        vm.onEvent(ClinicalAdmissionEvent.NextStep) // 5 -> 6
        advanceUntilIdle()
        assertEquals(6, vm.state.value.currentStep)

        // Step 6: toggle close record, evolution save succeeds
        vm.onEvent(ClinicalAdmissionEvent.ToggleCloseRecord(true, "Tratamiento de ingreso concluido."))
        coEvery { evolutionRepository.saveEvolution(any()) } returns Result.success(
            Evolution(
                id = "evo-1",
                recordId = "rec-456",
                doctorId = "12345678-1234-1234-1234-123456789abc",
                date = "2026-10-20",
                procedureTime = "10:00:00",
                description = "Evolución inicial",
                doctorName = "Carlos Doctor"
            )
        )
        coEvery { clinicalRecordRepository.closeRecord(any(), any(), any(), any()) } returns Result.success(Unit)

        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(7, vm.state.value.currentStep)
        assertNull(vm.state.value.errorMessage)
        coVerify(exactly = 1) {
            clinicalRecordRepository.closeRecord(
                recordId = "rec-456",
                doctorId = "12345678-1234-1234-1234-123456789abc",
                summary = "Tratamiento de ingreso concluido.",
                signatureUrl = any()
            )
        }
    }

    // =========================================================================
    // DRAFT PERSISTENCE & CLINICAL WORKLOAD SCENARIOS (TIER 3 & TIER 4)
    // =========================================================================

    @Test
    fun `Draft restoration with active doctor session populates patient data and maintains doctor identity`() = runTest {
        val doctorUuid = "99999999-8888-7777-6666-555555555555"
        val existingPatient = Patient(
            id = "pat-draft-1",
            dniCedula = "80123456",
            firstName = "Lucía",
            firstLastName = "Mora",
            phone = "3001234567"
        )
        val existingRecord = ClinicalRecord(
            id = "rec-draft-1",
            patientId = "pat-draft-1",
            mainDiagnosis = "Pulpitis irreversible"
        )
        val existingAnamnesis = Anamnesis(
            patientId = "pat-draft-1",
            alergias = true,
            alergiasDescripcion = "Penicilina"
        )

        coEvery { patientRepository.getPatientById("pat-draft-1") } returns Result.success(existingPatient)
        coEvery { clinicalRecordRepository.getRecordByPatientId("pat-draft-1") } returns Result.success(existingRecord)
        coEvery { anamnesisRepository.getAnamnesisByPatientId("pat-draft-1") } returns Result.success(existingAnamnesis)
        coEvery { odontogramRepository.getOdontogram("pat-draft-1") } returns Result.success(emptyList())
        coEvery { informedConsentRepository.getConsentByPatientId("pat-draft-1") } returns Result.success(null)
        coEvery { periodontalRepository.getExaminationByRecordId("rec-draft-1") } returns Result.success(null)

        val vm = createViewModelWithDoctor(
            doctorId = doctorUuid,
            doctorName = "Dr. Javier Solis",
            initialPatientId = "pat-draft-1"
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("pat-draft-1", state.patientId)
        assertEquals("80123456", state.dniCedula)
        assertEquals("Lucía", state.firstName)
        assertEquals("rec-draft-1", state.clinicalRecordId)
        assertEquals("Pulpitis irreversible", state.mainDiagnosis)
        assertTrue(state.alergias)
        assertEquals(doctorUuid, state.doctorId)
        assertEquals("Javier Solis", state.doctorName)
    }

    @Test
    fun `Doctor isolation Doctor B cannot operate under Doctor A credentials for existing patient`() = runTest {
        val doctorAUuid = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        val doctorBUuid = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"

        val existingPatient = Patient(id = "pat-isolated", dniCedula = "11223344", firstName = "Elena", firstLastName = "Pardo")
        val existingRecord = ClinicalRecord(id = "rec-isolated", patientId = "pat-isolated")

        coEvery { patientRepository.getPatientById("pat-isolated") } returns Result.success(existingPatient)
        coEvery { clinicalRecordRepository.getRecordByPatientId("pat-isolated") } returns Result.success(existingRecord)
        coEvery { anamnesisRepository.getAnamnesisByPatientId("pat-isolated") } returns Result.success(null)
        coEvery { odontogramRepository.getOdontogram("pat-isolated") } returns Result.success(emptyList())
        coEvery { informedConsentRepository.getConsentByPatientId("pat-isolated") } returns Result.success(null)
        coEvery { periodontalRepository.getExaminationByRecordId("rec-isolated") } returns Result.success(null)

        // Doctor A creates draft / opens patient
        val vmDoctorA = createViewModelWithDoctor(doctorId = doctorAUuid, doctorName = "Dr. Andres Alvarez", initialPatientId = "pat-isolated")
        advanceUntilIdle()
        assertEquals(doctorAUuid, vmDoctorA.state.value.doctorId)

        // Doctor B opens same patient -> must use Doctor B credentials, isolated from Doctor A
        val vmDoctorB = createViewModelWithDoctor(doctorId = doctorBUuid, doctorName = "Dra. Beatriz Bernal", initialPatientId = "pat-isolated")
        advanceUntilIdle()
        assertEquals(doctorBUuid, vmDoctorB.state.value.doctorId)
        assertNotEquals(vmDoctorA.state.value.doctorId, vmDoctorB.state.value.doctorId)
    }

    @Test
    fun `Full clinical admission happy path with authenticated doctor completes all 7 steps`() = runTest {
        val doctorUuid = "77777777-7777-7777-7777-777777777777"
        val vm = createViewModelWithDoctor(doctorId = doctorUuid, doctorName = "Dr. Mateo Martinez", licenseNumber = "OD-99001")
        advanceUntilIdle()

        // Mock all repo successes
        var capturedConsent: InformedConsent? = null
        var capturedExam: PeriodontalExamination? = null
        var capturedEvolution: Evolution? = null

        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-full", dniCedula = "99887766", firstName = "Valeria", firstLastName = "Torres")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-full", patientId = "pat-full")
        )
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-full"))
        coEvery { odontogramRepository.updateToothState(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { periodontalRepository.saveExamination(any()) } answers {
            capturedExam = firstArg()
            Result.success(requireNotNull(capturedExam))
        }
        coEvery { informedConsentRepository.saveConsent(any()) } answers {
            capturedConsent = firstArg()
            Result.success(requireNotNull(capturedConsent))
        }
        coEvery { evolutionRepository.saveEvolution(any()) } answers {
            capturedEvolution = firstArg()
            Result.success(requireNotNull(capturedEvolution))
        }
        coEvery { clinicalRecordRepository.closeRecord(any(), any(), any(), any()) } returns Result.success(Unit)

        // Step 1: Patient info
        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("99887766"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Valeria"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Torres"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.currentStep)

        // Step 2: Anamnesis
        vm.onEvent(ClinicalAdmissionEvent.ToggleAnamnesisQuestion("alergias", true))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.currentStep)

        // Step 3: Odontogram
        vm.onEvent(ClinicalAdmissionEvent.SelectOdontogramTooth(16))
        vm.onEvent(ClinicalAdmissionEvent.SelectConditionToApply(ToothCondition.CARIES))
        vm.onEvent(ClinicalAdmissionEvent.ApplySurfaceCondition(16, ToothSurface.OCLUSAL))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(4, vm.state.value.currentStep)

        // Step 4: Periodontal exam
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(5, vm.state.value.currentStep)
        assertEquals(doctorUuid, capturedExam?.doctorId)

        // Step 5: Treatment plan & consent
        vm.onEvent(ClinicalAdmissionEvent.UpdateDiagnoses("Caries dental profunda", "", "", "Restauración resina"))
        vm.onEvent(ClinicalAdmissionEvent.ConfirmConsentSignature("sig_data_valeria"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(6, vm.state.value.currentStep)
        assertEquals(doctorUuid, capturedConsent?.doctorId)
        assertEquals("OD-99001", capturedConsent?.doctorLicenseNumber)

        // Step 6: Evolution & close record
        val material = vm.state.value.materialsCatalog.first()
        vm.onEvent(ClinicalAdmissionEvent.SelectMaterialFromCatalog(material))
        vm.onEvent(ClinicalAdmissionEvent.AddMaterialUsed(1.0))
        vm.onEvent(ClinicalAdmissionEvent.UpdateEvolution("Restauración oclusal con resina filtek", "150000", "150000"))
        vm.onEvent(ClinicalAdmissionEvent.ConfirmEvolutionSignature("sig_evo_valeria"))
        vm.onEvent(ClinicalAdmissionEvent.ToggleCloseRecord(true, "Tratamiento finalizado satisfactoriamente."))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(7, vm.state.value.currentStep)
        assertEquals(doctorUuid, capturedEvolution?.doctorId)

        // Step 7: Finish admission
        vm.onEvent(ClinicalAdmissionEvent.FinishAdmission)
        advanceUntilIdle()
        assertTrue(vm.state.value.isFinished)
        assertNotNull(vm.state.value.successMessage)
    }
}
