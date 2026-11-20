package com.example.biodontos.data.repository

import androidx.room.Room
import com.example.biodontos.data.dto.ClinicalMaterialDto
import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.data.local.BiodontosDatabase
import com.example.biodontos.domain.model.DoctorId
import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoomRepositoryCacheTest {

    private lateinit var database: BiodontosDatabase
    private val supabaseClient: SupabaseClient = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            BiodontosDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `PatientRepositoryImpl caches remote patients in Room and serves them when offline`() = runTest(testDispatcher) {
        val remoteDto = PatientDto(
            id = "pat-cache-1",
            dniCedula = "10203040",
            firstName = "Sofía",
            firstLastName = "Ramírez",
            phone = "3001112233",
            email = "sofia@biodontos.com"
        )
        val repo = PatientRepositoryImpl(
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            patientDao = database.patientDao(),
            remotePatientsFetcher = { listOf(remoteDto) }
        )

        val onlineResult = repo.getPatients()
        assertTrue(onlineResult.isSuccess)
        assertEquals(1, onlineResult.getOrNull()?.size)

        // Simulate offline failure
        repo.remotePatientsFetcher = { throw IOException("Offline") }
        val offlineResult = repo.getPatients()
        assertTrue(offlineResult.isSuccess)
        assertEquals("Sofía", offlineResult.getOrNull()?.first()?.firstName)
        assertEquals("10203040", offlineResult.getOrNull()?.first()?.dniCedula)
    }

    @Test
    fun `MaterialRepositoryImpl caches remote materials in Room and serves active ones when offline`() = runTest(testDispatcher) {
        val remoteDto = ClinicalMaterialDto(
            id = "mat-cache-1",
            doctorId = "doc-1",
            name = "Resina Z350",
            invimaRegistration = "INVIMA-2026-001",
            batchLotNumber = "LOT-A1",
            manufacturingDate = "2025-01-01",
            expirationDate = "2028-01-01",
            isActive = true
        )
        val repo = MaterialRepositoryImpl(
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            materialDao = database.materialDao(),
            remoteMaterialsFetcher = { _, _ -> listOf(remoteDto) }
        )

        val onlineResult = repo.getMaterials(includeInactive = false, doctorId = DoctorId("doc-1"))
        assertTrue(onlineResult.isSuccess)
        assertEquals(1, onlineResult.getOrNull()?.size)

        // Simulate offline failure
        repo.remoteMaterialsFetcher = { _, _ -> throw IOException("Offline") }
        val offlineResult = repo.getMaterials(includeInactive = false, doctorId = DoctorId("doc-1"))
        assertTrue(offlineResult.isSuccess)
        assertEquals("Resina Z350", offlineResult.getOrNull()?.first()?.name)
        assertEquals("INVIMA-2026-001", offlineResult.getOrNull()?.first()?.invimaRegistration)
    }
}
