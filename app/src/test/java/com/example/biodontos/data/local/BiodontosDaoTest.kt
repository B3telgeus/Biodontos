package com.example.biodontos.data.local

import android.content.Context
import androidx.room.Room
import com.example.biodontos.data.local.*
import com.example.biodontos.data.local.dao.*
import com.example.biodontos.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Comprehensive Room DAO and Local Persistence test suite for Biodontos Android (Plan 1).
 *
 * Verifies:
 * - Tier 1: Primary CRUD operations for PatientDao, MaterialDao, and AdmissionDraftDao.
 * - Tier 2: Boundary conditions, DNI/name search filtering, and doctor draft isolation.
 * - Tier 3: SessionLocalCleaner purge behavior wiping all Room tables on logout.
 * - Tier 4: Clinical admission multi-step draft progress and multi-doctor isolation workloads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BiodontosDaoTest {

    private lateinit var db: BiodontosDatabase
    private lateinit var patientDao: PatientDao
    private lateinit var materialDao: MaterialDao
    private lateinit var admissionDraftDao: AdmissionDraftDao
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, BiodontosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        patientDao = db.patientDao()
        materialDao = db.materialDao()
        admissionDraftDao = db.admissionDraftDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }


    private fun createTestPatient(
        id: String = "pat-001",
        dniCedula: String = "12345678",
        firstName: String = "Carlos",
        firstLastName: String = "Perez",
        secondLastName: String = "Gomez",
        phone: String = "3001234567",
        email: String? = "carlos@example.com",
        birthDate: String = "1990-01-01",
        birthPlace: String = "Bogota D.C.",
        eps: String = "Sanitas",
        bloodGroup: String = "O",
        rhFactor: String = "+",
        consentSigned: Boolean = false
    ): PatientEntity {
        return PatientEntity(
            id = id,
            dniCedula = dniCedula,
            firstName = firstName,
            firstLastName = firstLastName,
            secondLastName = secondLastName,
            phone = phone,
            email = email,
            birthDate = birthDate,
            birthPlace = birthPlace,
            eps = eps,
            bloodGroup = bloodGroup,
            rhFactor = rhFactor,
            consentSigned = consentSigned
        )
    }

    private fun createTestMaterial(
        id: String = "mat-001",
        name: String = "Resina Compuesta A2",
        invimaRegistration: String = "INVIMA 2021DM-0012345",
        batchLotNumber: String = "LOT-2026-X",
        manufacturingDate: String = "2026-01-01",
        expirationDate: String = "2027-12-31",
        isActive: Boolean = true,
        doctorId: String? = null
    ): MaterialEntity {
        return MaterialEntity(
            id = id,
            doctorId = doctorId,
            name = name,
            invimaRegistration = invimaRegistration,
            batchLotNumber = batchLotNumber,
            manufacturingDate = manufacturingDate,
            expirationDate = expirationDate,
            isActive = isActive
        )
    }

    private fun createTestDraft(
        doctorId: String = "doc-001",
        patientId: String = "pat-001",
        currentStep: Int = 1,
        draftJson: String = """{"step1":{"dni":"12345678","name":"Carlos"}}""",
        updatedAt: Long = System.currentTimeMillis()
    ): AdmissionDraftEntity {
        return AdmissionDraftEntity(
            doctorId = doctorId,
            patientId = patientId,
            currentStep = currentStep,
            draftJson = draftJson,
            updatedAt = updatedAt
        )
    }


    @Test
    fun testPatientDao_insertAndRetrieveById_returnsMatchingEntity() = runTest {
        val patient = createTestPatient(
            id = "pat-100",
            dniCedula = "52987654",
            firstName = "Esperanza",
            firstLastName = "Gomez",
            phone = "3109876543",
            email = "esperanza@example.com"
        )

        patientDao.insert(patient)
        val retrieved = patientDao.getPatientById("pat-100")

        assertNotNull("Patient should be found in database", retrieved)
        assertEquals("pat-100", retrieved?.id)
        assertEquals("52987654", retrieved?.dniCedula)
        assertEquals("Esperanza", retrieved?.firstName)
        assertEquals("Gomez", retrieved?.firstLastName)
        assertEquals("3109876543", retrieved?.phone)
        assertEquals("esperanza@example.com", retrieved?.email)
    }

    @Test
    fun testPatientDao_getPatientById_nonExistentId_returnsNull() = runTest {
        val result = patientDao.getPatientById("non-existent-uuid")
        assertNull("Non-existent patient ID must return null", result)
    }

    @Test
    fun testPatientDao_insertExistingPatient_updatesRecordCleanly() = runTest {
        val original = createTestPatient(id = "pat-update-1", phone = "3001111111")
        patientDao.insert(original)

        val updated = original.copy(phone = "3009999999", email = "updated@example.com")
        patientDao.insert(updated)

        val result = patientDao.getPatientById("pat-update-1")
        assertNotNull(result)
        assertEquals("3009999999", result?.phone)
        assertEquals("updated@example.com", result?.email)
    }

    @Test
    fun testPatientDao_searchPatientsByDni_returnsExactAndPartialMatches() = runTest {
        val p1 = createTestPatient(id = "p1", dniCedula = "1017123456", firstName = "Andres")
        val p2 = createTestPatient(id = "p2", dniCedula = "1017999888", firstName = "Beatriz")
        val p3 = createTestPatient(id = "p3", dniCedula = "52345678", firstName = "Camilo")

        patientDao.insertAll(listOf(p1, p2, p3))

        val results = patientDao.searchPatients("1017")
        assertEquals("Search by DNI prefix 1017 should find 2 patients", 2, results.size)
        assertTrue(results.any { it.id == "p1" })
        assertTrue(results.any { it.id == "p2" })
        assertFalse(results.any { it.id == "p3" })
    }

    @Test
    fun testPatientDao_searchPatientsByName_caseInsensitive() = runTest {
        val p1 = createTestPatient(id = "p1", firstName = "Santiago", firstLastName = "Bejarano")
        val p2 = createTestPatient(id = "p2", firstName = "Mariana", firstLastName = "Santos")

        patientDao.insertAll(listOf(p1, p2))

        val lowerResults = patientDao.searchPatients("santiago")
        val upperResults = patientDao.searchPatients("SANTIAGO")
        val partialResults = patientDao.searchPatients("santi")

        assertEquals(1, lowerResults.size)
        assertEquals("p1", lowerResults.first().id)
        assertEquals(1, upperResults.size)
        assertEquals("p1", upperResults.first().id)
        assertEquals(1, partialResults.size)
        assertEquals("p1", partialResults.first().id)
    }

    @Test
    fun testPatientDao_searchPatientsByLastName_returnsMatches() = runTest {
        val p1 = createTestPatient(id = "p1", firstName = "Juan", firstLastName = "Restrepo")
        val p2 = createTestPatient(id = "p2", firstName = "Laura", firstLastName = "Restrepo")
        val p3 = createTestPatient(id = "p3", firstName = "Pedro", firstLastName = "Gonzalez")

        patientDao.insertAll(listOf(p1, p2, p3))

        val results = patientDao.searchPatients("Restrepo")
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == "p1" })
        assertTrue(results.any { it.id == "p2" })
    }

    @Test
    fun testPatientDao_searchPatients_noMatch_returnsEmptyList() = runTest {
        val p1 = createTestPatient(id = "p1", firstName = "Julian", dniCedula = "80123456")
        patientDao.insert(p1)

        val results = patientDao.searchPatients("nonexistentquery999")
        assertTrue("No match search should return empty list", results.isEmpty())
    }

    @Test
    fun testPatientDao_delete_removesEntity() = runTest {
        val patient = createTestPatient(id = "pat-del-1")
        patientDao.insert(patient)
        assertNotNull(patientDao.getPatientById("pat-del-1"))

        patientDao.delete(patient)
        assertNull("Patient must be null after deletion", patientDao.getPatientById("pat-del-1"))
    }


    @Test
    fun testMaterialDao_insertAllAndGetActiveMaterials_filtersOutInactive() = runTest {
        val m1 = createTestMaterial(id = "m1", name = "Resina 3M Filtek A2", isActive = true)
        val m2 = createTestMaterial(id = "m2", name = "Amalgama Dispersalloy (Discontinuado)", isActive = false)
        val m3 = createTestMaterial(id = "m3", name = "Anestesico Lidocaina 2% Epinefrina", isActive = true)
        val m4 = createTestMaterial(id = "m4", name = "Cemento Ionometro de Vidrio", isActive = true)

        materialDao.insertAll(listOf(m1, m2, m3, m4))

        val activeMaterials = materialDao.getActiveMaterials()
        assertEquals("Only 3 active materials should be returned", 3, activeMaterials.size)
        assertTrue(activeMaterials.any { it.id == "m1" })
        assertFalse(activeMaterials.any { it.id == "m2" })
        assertTrue(activeMaterials.any { it.id == "m3" })
        assertTrue(activeMaterials.any { it.id == "m4" })
    }

    @Test
    fun testMaterialDao_insertAll_emptyList_doesNotThrowAndReturnsEmpty() = runTest {
        materialDao.insertAll(emptyList())
        val active = materialDao.getActiveMaterials()
        assertTrue(active.isEmpty())
    }

    @Test
    fun testMaterialDao_getActiveMaterials_whenAllInactive_returnsEmptyList() = runTest {
        val m1 = createTestMaterial(id = "m1", isActive = false)
        val m2 = createTestMaterial(id = "m2", isActive = false)

        materialDao.insertAll(listOf(m1, m2))
        val active = materialDao.getActiveMaterials()
        assertTrue("When all materials are inactive, active list must be empty", active.isEmpty())
    }

    @Test
    fun testMaterialDao_preservesInvimaAndBatchLotInformation() = runTest {
        val material = createTestMaterial(
            id = "m-invima",
            name = "Sutura Seda Negra 3-0",
            invimaRegistration = "INVIMA 2023DM-00998877",
            batchLotNumber = "LOT-OCT26-B9"
        )
        materialDao.insertAll(listOf(material))

        val active = materialDao.getActiveMaterials()
        val retrieved = active.firstOrNull { it.id == "m-invima" }

        assertNotNull(retrieved)
        assertEquals("INVIMA 2023DM-00998877", retrieved?.invimaRegistration)
        assertEquals("LOT-OCT26-B9", retrieved?.batchLotNumber)
    }


    @Test
    fun testAdmissionDraftDao_saveDraft_insertsAndRetrievesCorrectly() = runTest {
        val draft = createTestDraft(
            doctorId = "doc-alpha",
            patientId = "pat-beta",
            currentStep = 2,
            draftJson = """{"anamnesis":{"diabetes":false,"alergias":true}}"""
        )

        admissionDraftDao.insertDraft(draft)
        val retrieved = admissionDraftDao.getDraft("doc-alpha", "pat-beta").firstOrNull()

        assertNotNull("Saved draft must be retrievable", retrieved)
        assertEquals("doc-alpha", retrieved?.doctorId)
        assertEquals("pat-beta", retrieved?.patientId)
        assertEquals(2, retrieved?.currentStep)
        assertEquals("""{"anamnesis":{"diabetes":false,"alergias":true}}""", retrieved?.draftJson)
    }

    @Test
    fun testAdmissionDraftDao_getDraft_nonExistent_returnsNull() = runTest {
        val retrieved = admissionDraftDao.getDraft("doc-ghost", "pat-ghost").firstOrNull()
        assertNull("Non-existent draft query must emit null", retrieved)
    }

    @Test
    fun testAdmissionDraftDao_updateDraft_overwritesPreviousDataCleanly() = runTest {
        val initialDraft = createTestDraft(
            doctorId = "doc-01",
            patientId = "pat-01",
            currentStep = 1,
            draftJson = """{"step":1}"""
        )
        admissionDraftDao.insertDraft(initialDraft)

        val updatedDraft = createTestDraft(
            doctorId = "doc-01",
            patientId = "pat-01",
            currentStep = 4,
            draftJson = """{"step":4,"odontogram":{"11":"CARIES"}}"""
        )
        admissionDraftDao.insertDraft(updatedDraft)

        val retrieved = admissionDraftDao.getDraft("doc-01", "pat-01").firstOrNull()
        assertNotNull(retrieved)
        assertEquals(4, retrieved?.currentStep)
        assertEquals("""{"step":4,"odontogram":{"11":"CARIES"}}""", retrieved?.draftJson)
    }

    @Test
    fun testAdmissionDraftDao_deleteDraft_removesRecordOnCompletion() = runTest {
        val draft = createTestDraft(doctorId = "doc-del", patientId = "pat-del")
        admissionDraftDao.insertDraft(draft)
        assertNotNull(admissionDraftDao.getDraft("doc-del", "pat-del").firstOrNull())

        admissionDraftDao.deleteDraft("doc-del", "pat-del")
        val afterDelete = admissionDraftDao.getDraft("doc-del", "pat-del").firstOrNull()
        assertNull("Draft must be null after deleteDraft", afterDelete)
    }

    @Test
    fun testAdmissionDraftDao_draftIsolation_doctorACannotSeeDoctorBDraftForSamePatient() = runTest {
        val patientId = "patient-shared-uuid"
        val draftDocA = createTestDraft(
            doctorId = "doc-A",
            patientId = patientId,
            currentStep = 3,
            draftJson = """{"savedBy":"Doctor A"}"""
        )
        val draftDocB = createTestDraft(
            doctorId = "doc-B",
            patientId = patientId,
            currentStep = 5,
            draftJson = """{"savedBy":"Doctor B"}"""
        )

        admissionDraftDao.insertDraft(draftDocA)

        val docBSawDocA = admissionDraftDao.getDraft("doc-B", patientId).firstOrNull()
        assertNull("Doctor B must not see Doctor A's uncommitted draft for patient X", docBSawDocA)

        admissionDraftDao.insertDraft(draftDocB)

        val retrievedA = admissionDraftDao.getDraft("doc-A", patientId).firstOrNull()
        val retrievedB = admissionDraftDao.getDraft("doc-B", patientId).firstOrNull()

        assertNotNull(retrievedA)
        assertNotNull(retrievedB)
        assertEquals(3, retrievedA?.currentStep)
        assertEquals("""{"savedBy":"Doctor A"}""", retrievedA?.draftJson)
        assertEquals(5, retrievedB?.currentStep)
        assertEquals("""{"savedBy":"Doctor B"}""", retrievedB?.draftJson)
    }

    @Test
    fun testAdmissionDraftDao_draftIsolation_sameDoctorDifferentPatients() = runTest {
        val docId = "doc-isolated"
        val draftPat1 = createTestDraft(doctorId = docId, patientId = "pat-1", currentStep = 2)
        admissionDraftDao.insertDraft(draftPat1)

        val draftPat2 = admissionDraftDao.getDraft(docId, "pat-2").firstOrNull()
        assertNull("Draft for pat-2 should not be returned when only pat-1 exists", draftPat2)
    }


    @Test
    fun testSessionLocalCleaner_wipesAllRoomTables_leavingZeroRowsAcrossEntities() = runTest {
        patientDao.insertAll(
            listOf(
                createTestPatient(id = "p-wipe-1"),
                createTestPatient(id = "p-wipe-2")
            )
        )
        materialDao.insertAll(
            listOf(
                createTestMaterial(id = "m-wipe-1", isActive = true),
                createTestMaterial(id = "m-wipe-2", isActive = true)
            )
        )
        admissionDraftDao.insertDraft(createTestDraft(doctorId = "d-wipe-1", patientId = "p-wipe-1"))
        admissionDraftDao.insertDraft(createTestDraft(doctorId = "d-wipe-2", patientId = "p-wipe-2"))

        assertNotNull(patientDao.getPatientById("p-wipe-1"))
        assertEquals(2, materialDao.getActiveMaterials().size)
        assertNotNull(admissionDraftDao.getDraft("d-wipe-1", "p-wipe-1").firstOrNull())

        val cleaner = SessionLocalCleaner(db, context)
        cleaner.clearSessionData()

        assertNull("Patient table must be wiped", patientDao.getPatientById("p-wipe-1"))
        assertNull("Patient table must be wiped", patientDao.getPatientById("p-wipe-2"))
        assertTrue("Patients search must return empty list", patientDao.searchPatients("").isEmpty())

        assertTrue("Materials table must be wiped", materialDao.getActiveMaterials().isEmpty())

        assertNull(
            "Draft table must be wiped",
            admissionDraftDao.getDraft("d-wipe-1", "p-wipe-1").firstOrNull()
        )
        assertNull(
            "Draft table must be wiped",
            admissionDraftDao.getDraft("d-wipe-2", "p-wipe-2").firstOrNull()
        )
    }

    @Test
    fun testSessionLocalCleaner_calledOnEmptyDatabase_completesWithoutError() = runTest {
        val cleaner = SessionLocalCleaner(db, context)
        cleaner.clearSessionData()
        assertTrue(patientDao.searchPatients("").isEmpty())
        assertTrue(materialDao.getActiveMaterials().isEmpty())
    }

    @Test
    fun testSessionLocalCleaner_repeatedConsecutiveCalls_areIdempotent() = runTest {
        val cleaner = SessionLocalCleaner(db, context)
        patientDao.insert(createTestPatient(id = "idempotent-pat"))

        cleaner.clearSessionData()
        cleaner.clearSessionData()
        cleaner.clearSessionData()

        assertNull(patientDao.getPatientById("idempotent-pat"))
    }


    @Test
    fun testRealisticAdmissionDraftProgress_throughAllSixSteps() = runTest {
        val doctorId = "doctor-dr-suarez"
        val patientId = "patient-pedro-gomez"

        val step1 = createTestDraft(
            doctorId = doctorId,
            patientId = patientId,
            currentStep = 1,
            draftJson = """{"step":1,"dni":"1029384756","firstName":"Pedro","firstLastName":"Gomez"}"""
        )
        admissionDraftDao.insertDraft(step1)
        assertEquals(1, admissionDraftDao.getDraft(doctorId, patientId).first()?.currentStep)

        val step2 = createTestDraft(
            doctorId = doctorId,
            patientId = patientId,
            currentStep = 2,
            draftJson = """{"step":2,"diabetes":false,"cardiopatias":false,"alergias":true,"alergiasDesc":"Penicilina"}"""
        )
        admissionDraftDao.insertDraft(step2)
        assertEquals(2, admissionDraftDao.getDraft(doctorId, patientId).first()?.currentStep)

        val step3 = createTestDraft(
            doctorId = doctorId,
            patientId = patientId,
            currentStep = 3,
            draftJson = """{"step":3,"teeth":{"16":{"MESIAL":"CARIES"},"21":{"OCCLUSAL":"FRACTURE"}}}"""
        )
        admissionDraftDao.insertDraft(step3)
        assertEquals(3, admissionDraftDao.getDraft(doctorId, patientId).first()?.currentStep)

        val step4 = createTestDraft(
            doctorId = doctorId,
            patientId = patientId,
            currentStep = 4,
            draftJson = """{"step":4,"periodontalSites":{"16_DISTAL":{"pocketDepth":4,"bleeding":true}}}"""
        )
        admissionDraftDao.insertDraft(step4)
        assertEquals(4, admissionDraftDao.getDraft(doctorId, patientId).first()?.currentStep)

        val step5 = createTestDraft(
            doctorId = doctorId,
            patientId = patientId,
            currentStep = 5,
            draftJson = """{"step":5,"consentSigned":true,"patientSignature":"data:image/png;base64,iVBORw0KGgo..."}"""
        )
        admissionDraftDao.insertDraft(step5)
        assertEquals(5, admissionDraftDao.getDraft(doctorId, patientId).first()?.currentStep)

        val step6 = createTestDraft(
            doctorId = doctorId,
            patientId = patientId,
            currentStep = 6,
            draftJson = """{"step":6,"evolutionNote":"Restauracion resina resuelta sin novedades","materialsUsed":[{"materialId":"mat-001","quantity":1}]}"""
        )
        admissionDraftDao.insertDraft(step6)
        val finalDraft = admissionDraftDao.getDraft(doctorId, patientId).first()
        assertEquals(6, finalDraft?.currentStep)
        assertTrue(finalDraft?.draftJson?.contains("Restauracion resina") == true)

        admissionDraftDao.deleteDraft(doctorId, patientId)
        assertNull("Draft must be deleted once admission is finalized", admissionDraftDao.getDraft(doctorId, patientId).firstOrNull())
    }

    @Test
    fun testMultiDoctorConcurrentDraftSwitchingWorkload() = runTest {
        val doctorA = "doc-pediatrician"
        val doctorB = "doc-endodontist"
        val patientAlpha = "pat-child-001"
        val patientBeta = "pat-adult-002"

        admissionDraftDao.insertDraft(createTestDraft(doctorId = doctorA, patientId = patientAlpha, currentStep = 2))

        admissionDraftDao.insertDraft(createTestDraft(doctorId = doctorB, patientId = patientBeta, currentStep = 4))

        assertNull(admissionDraftDao.getDraft(doctorA, patientBeta).firstOrNull())
        assertNull(admissionDraftDao.getDraft(doctorB, patientAlpha).firstOrNull())

        assertEquals(2, admissionDraftDao.getDraft(doctorA, patientAlpha).first()?.currentStep)
        assertEquals(4, admissionDraftDao.getDraft(doctorB, patientBeta).first()?.currentStep)
    }
}
