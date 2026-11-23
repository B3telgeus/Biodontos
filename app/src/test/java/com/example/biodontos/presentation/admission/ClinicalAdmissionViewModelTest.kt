package com.example.biodontos.presentation.admission

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.*
import com.example.biodontos.domain.model.history.AuditAction
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
    private val clinicalAuditRepository: ClinicalAuditRepository = mockk(relaxed = true)

    private lateinit var viewModel: ClinicalAdmissionViewModel

    private fun createViewModelWithDoctor(
        doctorId: String = "12345678-1234-1234-1234-123456789abc",
        doctorName: String = "Dr. Carlos Valderrama",
        licenseNumber: String = "OD-12345-COL",
        initialPatientId: String? = null,
        doctorUseCase: GetCurrentDoctorUseCase? = null,
        doctorRepo: DoctorRepository? = null,
        auditRepository: ClinicalAuditRepository? = null
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
            guardianRepository = guardianRepository,
            initialPatientId = initialPatientId,
            getCurrentDoctorUseCase = useCase,
            clinicalAuditRepository = auditRepository ?: clinicalAuditRepository
        )
    }

    @Before
    fun setUp() {
        viewModel = ClinicalAdmissionViewModel(
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
            guardianRepository = guardianRepository,
            clinicalAuditRepository = clinicalAuditRepository
        )
    }

    @Test
    fun `Initial state starts at Step 1`() = runTest {
        val state = viewModel.state.value
        assertEquals(1, state.currentStep)
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

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1020304050"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Carlos"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.currentStep)

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

        val vm = ClinicalAdmissionViewModel(
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
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
        assertTrue(s.alergias)
        assertTrue(s.diabetes)
        assertFalse(s.isLoading)
    }

    @Test
    fun `Step 1 saves guardian and associates guardian_id to patient when hasGuardian is true`() = runTest {
        val savedGuardian = Guardian(
            id = "g-uuid-999",
            firstName = "Elena",
            lastName = "Gómez",
            phone = "3109998877",
            address = "Calle 100 # 15-20"
        )
        val savedPatient = Patient(
            id = "pat-child-1",
            dniCedula = "1199887766",
            firstName = "Mateo",
            firstLastName = "Gómez",
            guardianId = "g-uuid-999"
        )

        coEvery { guardianRepository.saveGuardian(any()) } returns Result.success(savedGuardian)
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(savedPatient)
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-child-1", patientId = "pat-child-1")
        )

        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("1199887766"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Mateo"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Gómez"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateBirthDate("2018-03-15"))
        vm.onEvent(ClinicalAdmissionEvent.ToggleHasGuardian(true))
        vm.onEvent(
            ClinicalAdmissionEvent.UpdateGuardian(
                relationship = "Madre",
                firstName = "Elena",
                lastName = "Gómez",
                phone = "3109998877",
                address = "Calle 100 # 15-20"
            )
        )

        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.currentStep)
        coVerify(exactly = 1) { guardianRepository.saveGuardian(any()) }
        coVerify(exactly = 1) {
            patientRepository.createOrUpdatePatient(
                match { it.guardianId == "g-uuid-999" }
            )
        }
    }

    @Test
    fun `Admission save blocked when getCurrentDoctorUseCase returns null`() = runTest {
        val doctorUseCase: GetCurrentDoctorUseCase = mockk()
        coEvery { doctorUseCase() } returns Result.success(null)

        val vm = ClinicalAdmissionViewModel(
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
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

        val vmDoctorA = createViewModelWithDoctor(doctorId = doctorAUuid, doctorName = "Dr. Andres Alvarez", initialPatientId = "pat-isolated")
        advanceUntilIdle()
        assertEquals(doctorAUuid, vmDoctorA.state.value.doctorId)

        val vmDoctorB = createViewModelWithDoctor(doctorId = doctorBUuid, doctorName = "Dra. Beatriz Bernal", initialPatientId = "pat-isolated")
        advanceUntilIdle()
        assertEquals(doctorBUuid, vmDoctorB.state.value.doctorId)
        assertNotEquals(vmDoctorA.state.value.doctorId, vmDoctorB.state.value.doctorId)
    }

    @Test
    fun `Full clinical admission happy path with authenticated doctor completes 2 steps, emits completion result and records audit`() = runTest {
        val doctorUuid = "77777777-7777-7777-7777-777777777777"
        val auditRepo: ClinicalAuditRepository = mockk(relaxed = true)
        coEvery { auditRepo.recordAudit(any(), any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)

        val vm = createViewModelWithDoctor(
            doctorId = doctorUuid,
            doctorName = "Dr. Mateo Martinez",
            licenseNumber = "OD-99001",
            auditRepository = auditRepo
        )
        advanceUntilIdle()

        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-full", dniCedula = "99887766", firstName = "Valeria", firstLastName = "Torres")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-full", patientId = "pat-full")
        )
        coEvery { anamnesisRepository.saveAnamnesis(any()) } returns Result.success(Anamnesis(patientId = "pat-full"))

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("99887766"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Valeria"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Torres"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.currentStep)

        vm.onEvent(ClinicalAdmissionEvent.ToggleAnamnesisQuestion("alergias", true))
        vm.onEvent(ClinicalAdmissionEvent.UpdateAnamnesisOptionNote("alergias", "Alérgica a la penicilina"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        val completion = vm.state.value.completionResult
        assertNotNull(completion)
        assertEquals("pat-full", completion?.patientId)
        assertEquals("Valeria Torres", completion?.patientFullName)
        assertEquals("99887766", completion?.dniCedula)
        assertTrue(completion?.isNewAdmission == true)

        coVerify(exactly = 1) {
            auditRepo.recordAudit(
                patientId = "pat-full",
                entityTable = "clinical_records",
                entityId = "rec-full",
                action = AuditAction.CREATE,
                changedFields = match { it.contains("patient_admission") && it.contains("anamnesis") },
                actorUserId = "user-77777777-7777-7777-7777-777777777777",
                actorName = "Mateo Martinez"
            )
        }

        // Test dismiss dialog
        vm.onEvent(ClinicalAdmissionEvent.DismissSuccessDialog)
        assertNull(vm.state.value.completionResult)
    }

    @Test
    fun `SavedStateHandle restores currentStep and patientId without storing large payloads`() = runTest {
        val handle = androidx.lifecycle.SavedStateHandle(
            mapOf(
                ClinicalAdmissionViewModel.KEY_CURRENT_STEP to 2,
                ClinicalAdmissionViewModel.KEY_PATIENT_ID to "pat-saved-123",
                ClinicalAdmissionViewModel.KEY_CLINICAL_RECORD_ID to "rec-saved-456"
            )
        )

        val vm = ClinicalAdmissionViewModel(
            patientRepository = patientRepository,
            clinicalRecordRepository = clinicalRecordRepository,
            anamnesisRepository = anamnesisRepository,
            guardianRepository = guardianRepository,
            savedStateHandle = handle
        )

        assertEquals(2, vm.state.value.currentStep)
        assertEquals("pat-saved-123", vm.state.value.patientId)
        assertEquals("rec-saved-456", vm.state.value.clinicalRecordId)
    }

    @Test
    fun `Step 2 persists notes only for active anamnesis options`() = runTest {
        val vm = createViewModelWithDoctor()
        advanceUntilIdle()

        var capturedAnamnesis: Anamnesis? = null
        coEvery { patientRepository.createOrUpdatePatient(any()) } returns Result.success(
            Patient(id = "pat-notes", dniCedula = "12345678", firstName = "Ana", firstLastName = "Ruiz")
        )
        coEvery { clinicalRecordRepository.saveRecord(any()) } returns Result.success(
            ClinicalRecord(id = "rec-notes", patientId = "pat-notes")
        )
        coEvery { anamnesisRepository.saveAnamnesis(any()) } answers {
            capturedAnamnesis = firstArg()
            Result.success(firstArg())
        }

        vm.onEvent(ClinicalAdmissionEvent.UpdateDni("12345678"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstName("Ana"))
        vm.onEvent(ClinicalAdmissionEvent.UpdateFirstLastName("Ruiz"))
        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.currentStep)

        vm.onEvent(ClinicalAdmissionEvent.ToggleAnamnesisQuestion("diabetes", true))
        vm.onEvent(ClinicalAdmissionEvent.UpdateAnamnesisOptionNote("diabetes", "Tipo 2 controlada"))
        vm.onEvent(ClinicalAdmissionEvent.ToggleAnamnesisQuestion("alergias", true))
        vm.onEvent(ClinicalAdmissionEvent.UpdateAnamnesisOptionNote("alergias", "Penicilina"))
        vm.onEvent(ClinicalAdmissionEvent.ToggleAnamnesisQuestion("alergias", false))

        vm.onEvent(ClinicalAdmissionEvent.NextStep)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.currentStep)
        assertNotNull(vm.state.value.completionResult)
        assertEquals(mapOf("diabetes" to "Tipo 2 controlada"), capturedAnamnesis?.optionNotes)
    }
}
