package com.example.biodontos.domain.usecase.auth

import com.example.biodontos.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetPasswordUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = ResetPasswordUseCase(authRepository)

    @Test
    fun resetPasswordWithValidEmailCallsRepositoryAndReturnsSuccess() = runTest {
        coEvery { authRepository.resetPassword("paciente@correo.com") } returns Result.success(Unit)

        val result = useCase("paciente@correo.com")

        assertTrue(result.isSuccess)
    }

    @Test
    fun resetPasswordWithBlankEmailReturnsFailure() = runTest {
        val result = useCase("")

        assertTrue(result.isFailure)
        assertEquals("Ingresa un correo electrónico válido", result.exceptionOrNull()?.message)
    }
}
