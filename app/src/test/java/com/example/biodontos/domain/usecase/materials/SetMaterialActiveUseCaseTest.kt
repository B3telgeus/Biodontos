package com.example.biodontos.domain.usecase.materials

import com.example.biodontos.domain.model.MaterialError
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.repository.MaterialRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SetMaterialActiveUseCaseTest {

    private val materialRepository: MaterialRepository = mockk()
    private lateinit var useCase: SetMaterialActiveUseCase

    private val materialId = MaterialId("mat-test-1")

    @Before
    fun setUp() {
        useCase = SetMaterialActiveUseCase(materialRepository)
    }

    @Test
    fun `deactivating material delegates to repository and returns Right Unit on success`() = runTest {
        coEvery { materialRepository.setMaterialActive(materialId, false) } returns Result.success(Unit)

        val result = useCase(materialId, isActive = false)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { materialRepository.setMaterialActive(materialId, false) }
    }

    @Test
    fun `reactivating material delegates to repository and returns Right Unit on success`() = runTest {
        coEvery { materialRepository.setMaterialActive(materialId, true) } returns Result.success(Unit)

        val result = useCase(materialId, isActive = true)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { materialRepository.setMaterialActive(materialId, true) }
    }

    @Test
    fun `invoking with string id delegates correctly`() = runTest {
        coEvery { materialRepository.setMaterialActive(materialId, false) } returns Result.success(Unit)

        val result = useCase("mat-test-1", isActive = false)

        assertTrue(result.isRight())
        coVerify(exactly = 1) { materialRepository.setMaterialActive(materialId, false) }
    }

    @Test
    fun `invoking with blank string id returns PersistenceError`() = runTest {
        val result = useCase("   ", isActive = false)

        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is MaterialError.PersistenceError)
    }

    @Test
    fun `repository failure returns MaterialError PersistenceError`() = runTest {
        coEvery { materialRepository.setMaterialActive(materialId, false) } returns Result.failure(
            RuntimeException("Connection closed by server")
        )

        val result = useCase(materialId, isActive = false)

        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertTrue(error is MaterialError.PersistenceError)
            val persistenceError = error as MaterialError.PersistenceError
            assertTrue(persistenceError.reason.contains("Connection closed by server"))
        }
    }
}
