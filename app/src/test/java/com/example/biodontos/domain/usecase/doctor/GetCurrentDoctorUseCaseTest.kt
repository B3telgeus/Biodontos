package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AuthRepository
import com.example.biodontos.domain.repository.DoctorRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetCurrentDoctorUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val doctorRepository: DoctorRepository = mockk()
    private lateinit var useCase: GetCurrentDoctorUseCase

    private val testDoctor = Doctor(
        id = DoctorId("doc-123"),
        userId = "user-doc-123",
        firstName = "Andrés",
        lastName = "Castro",
        specialty = "Ortodoncia",
        licenseNumber = "COL-9876",
        email = "andres@test.com"
    )

    @Before
    fun setUp() {
        useCase = GetCurrentDoctorUseCase(authRepository, doctorRepository)
    }

    @Test
    fun `when no active session, returns failure`() = runTest {
        coEvery { authRepository.getCurrentSession() } returns null

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("No hay una sesión activa", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when session role is not doctor, returns failure`() = runTest {
        val patientSession = UserSession(
            id = "user-pat-1",
            email = "pat@test.com",
            role = UserRole.PATIENT,
            displayName = "Paciente"
        )
        coEvery { authRepository.getCurrentSession() } returns patientSession

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("El usuario actual no tiene rol de doctor", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when session is doctor, fetches doctor by userId successfully`() = runTest {
        val doctorSession = UserSession(
            id = "user-doc-123",
            email = "andres@test.com",
            role = UserRole.DOCTOR,
            displayName = "Dr. Andrés Castro"
        )
        coEvery { authRepository.getCurrentSession() } returns doctorSession
        coEvery { doctorRepository.getDoctorByUserId("user-doc-123") } returns Result.success(testDoctor)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals("doc-123", result.getOrNull()?.id?.value)
        assertEquals("Dr. Andrés Castro", result.getOrNull()?.displayName)
    }
}
