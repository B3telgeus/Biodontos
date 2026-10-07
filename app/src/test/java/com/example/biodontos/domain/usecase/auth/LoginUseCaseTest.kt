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

class LoginUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = LoginUseCase(authRepository)

    @Test
    fun loginWithValidCredentialsReturnsSuccess() = runTest {
        val expectedSession = UserSession("uid-123", "dr.smith@biodontos.com", UserRole.DOCTOR, "Dr. Smith")
        coEvery { authRepository.login("dr.smith@biodontos.com", "Password123") } returns Result.success(expectedSession)

        val result = useCase("dr.smith@biodontos.com", "Password123")

        assertTrue(result.isSuccess)
        assertEquals(expectedSession, result.getOrNull())
    }

    @Test
    fun loginWithInvalidEmailReturnsFailureWithoutCallingRepository() = runTest {
        val result = useCase("invalid-email", "Password123")

        assertTrue(result.isFailure)
        assertEquals("El correo electrónico no es válido", result.exceptionOrNull()?.message)
    }

    @Test
    fun loginWithBlankPasswordReturnsFailure() = runTest {
        val result = useCase("dr.smith@biodontos.com", "")

        assertTrue(result.isFailure)
        assertEquals("La contraseña no puede estar vacía", result.exceptionOrNull()?.message)
    }
}
