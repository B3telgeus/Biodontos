package com.example.biodontos.domain.usecase.patients

import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.PatientRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPatientsUseCaseTest {

    private val repository = mockk<PatientRepository>()
    private val useCase = GetPatientsUseCase(repository)

    @Test
    fun invokeCallsRepositoryAndReturnsSuccess() = runBlocking {
        val patients = listOf(
            Patient("1", "1010", "John", "Doe", true, false)
        )
        coEvery { repository.getPatients() } returns Result.success(patients)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(patients, result.getOrNull())
    }

    @Test
    fun invokeReturnsFailureWhenRepositoryFails() = runBlocking {
        val exception = Exception("Network error")
        coEvery { repository.getPatients() } returns Result.failure(exception)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
