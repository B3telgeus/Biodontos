package com.example.biodontos.domain.usecase.auth

import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemainingAuthUseCasesTest {

    private val authRepository: AuthRepository = mockk()

    @Test
    fun `RegisterPatientAuthUseCase validates inputs and delegates on valid data`() = runTest {
        val useCase = RegisterPatientAuthUseCase(authRepository)
        val expectedSession = UserSession("usr-1", "ana@biodontos.com", UserRole.PATIENT, "Ana")

        coEvery {
            authRepository.registerPatient(
                "ana@biodontos.com",
                "secret123",
                "Ana",
                "Martínez",
                "10203040",
                "3001112233"
            )
        } returns Result.success(expectedSession)

        val invalidName = useCase("ana@biodontos.com", "secret123", "", "Martínez", "10203040")
        assertTrue(invalidName.isFailure)

        val invalidPassword = useCase("ana@biodontos.com", "123", "Ana", "Martínez", "10203040")
        assertTrue(invalidPassword.isFailure)

        val validResult = useCase("ana@biodontos.com", "secret123", "Ana", "Martínez", "10203040", "3001112233")
        assertTrue(validResult.isSuccess)
        assertEquals(expectedSession, validResult.getOrNull())
    }

    @Test
    fun `GetCurrentSessionUseCase returns current session from repository`() = runTest {
        val expectedSession = UserSession("usr-2", "doc@biodontos.com", UserRole.DOCTOR, "Dr. Juan")
        coEvery { authRepository.getCurrentSession() } returns expectedSession

        val useCase = GetCurrentSessionUseCase(authRepository)
        val session = useCase()

        assertEquals(expectedSession, session)
    }

    @Test
    fun `LogoutUseCase calls logout on repository`() = runTest {
        coEvery { authRepository.logout() } returns Result.success(Unit)

        val useCase = LogoutUseCase(authRepository)
        val result = useCase()

        assertTrue(result.isSuccess)
    }
}
