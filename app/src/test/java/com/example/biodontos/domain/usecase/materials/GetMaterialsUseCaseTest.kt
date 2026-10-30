package com.example.biodontos.domain.usecase.materials

import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.repository.MaterialRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetMaterialsUseCaseTest {

    private val materialRepository: MaterialRepository = mockk()
    private lateinit var useCase: GetMaterialsUseCase

    private val sampleMaterial = ClinicalMaterial(
        id = MaterialId("mat-1"),
        doctorId = DoctorId("doc-1"),
        name = "Resina Z250",
        invimaRegistration = "INV-100",
        batchLotNumber = "L-100",
        manufacturingDate = LocalDate.of(2024, 1, 1),
        expirationDate = LocalDate.of(2027, 1, 1),
        isActive = true
    )

    @Before
    fun setUp() {
        useCase = GetMaterialsUseCase(materialRepository)
    }

    @Test
    fun `invoke with defaults returns list of active materials on success`() = runTest {
        coEvery {
            materialRepository.getMaterials(includeInactive = false, doctorId = null)
        } returns Result.success(listOf(sampleMaterial))

        val result = useCase()

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(1, list.size)
            assertEquals("Resina Z250", list.first().name)
        }
        coVerify(exactly = 1) {
            materialRepository.getMaterials(includeInactive = false, doctorId = null)
        }
    }

    @Test
    fun `invoke with includeInactive and doctorId passes parameters to repository`() = runTest {
        val doctorId = DoctorId("doc-777")
        coEvery {
            materialRepository.getMaterials(includeInactive = true, doctorId = doctorId)
        } returns Result.success(listOf(sampleMaterial))

        val result = useCase(includeInactive = true, doctorId = doctorId)

        assertTrue(result.isRight())
        result.onRight { list ->
            assertEquals(1, list.size)
        }
        coVerify(exactly = 1) {
            materialRepository.getMaterials(includeInactive = true, doctorId = doctorId)
        }
    }

    @Test
    fun `invoke with string doctorId delegates properly to DoctorId`() = runTest {
        val doctorId = DoctorId("doc-888")
        coEvery {
            materialRepository.getMaterials(includeInactive = false, doctorId = doctorId)
        } returns Result.success(listOf(sampleMaterial))

        val result = useCase(includeInactive = false, doctorId = "doc-888")

        assertTrue(result.isRight())
        coVerify(exactly = 1) {
            materialRepository.getMaterials(includeInactive = false, doctorId = doctorId)
        }
    }

    @Test
    fun `invoke returns NetworkOrPersistenceFailure when repository fails`() = runTest {
        coEvery {
            materialRepository.getMaterials(includeInactive = false, doctorId = null)
        } returns Result.failure(RuntimeException("Supabase connection timeout"))

        val result = useCase()

        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is DomainError.NetworkOrPersistenceFailure)
            val failure = error as DomainError.NetworkOrPersistenceFailure
            assertTrue(failure.reason.contains("Supabase connection timeout"))
        }
    }
}
