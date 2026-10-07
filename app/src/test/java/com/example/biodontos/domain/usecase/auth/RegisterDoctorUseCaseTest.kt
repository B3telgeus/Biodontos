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

class RegisterDoctorUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val useCase = RegisterDoctorUseCase(authRepository)

    @Test
    fun registerDoctorWithValidDataReturnsSuccess() = runTest {
        val expectedSession = UserSession("doc-1", "doc@clinic.com", UserRole.DOCTOR, "Dr. Ana")
        coEvery {
            authRepository.registerDoctor("doc@clinic.com", "SecurePass", "Ana", "Torres", "MP-9876", "Ortodoncia", null)
        } returns Result.success(expectedSession)

        val result = useCase(
            email = "doc@clinic.com",
            password = "SecurePass",
            firstName = "Ana",
            lastName = "Torres",
            licenseNumber = "MP-9876",
            specialty = "Ortodoncia"
        )

        assertTrue(result.isSuccess)
        assertEquals(expectedSession, result.getOrNull())
    }

    @Test
    fun registerDoctorWithShortPasswordReturnsFailure() = runTest {
        val result = useCase(
            email = "doc@clinic.com",
            password = "123",
            firstName = "Ana",
            lastName = "Torres",
            licenseNumber = "MP-9876",
            specialty = "Ortodoncia"
        )

        assertTrue(result.isFailure)
        assertEquals("La contraseña debe tener al menos 6 caracteres", result.exceptionOrNull()?.message)
    }
}
