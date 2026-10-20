package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.repository.AppointmentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleAppointmentConfirmationUseCaseTest {

    private val appointmentRepository: AppointmentRepository = mockk()
    private lateinit var useCase: ToggleAppointmentConfirmationUseCase

    @Before
    fun setUp() {
        useCase = ToggleAppointmentConfirmationUseCase(appointmentRepository)
    }

    @Test
    fun `when appointmentId is blank, returns failure`() = runTest {
        val result = useCase("", true)
        assertTrue(result.isFailure)
        assertEquals("El identificador de la cita no puede estar vacío", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when appointmentId is valid and confirming, calls repository with true`() = runTest {
        coEvery { appointmentRepository.updateConfirmationStatus("appt-10", true) } returns Result.success(Unit)

        val result = useCase("appt-10", true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { appointmentRepository.updateConfirmationStatus("appt-10", true) }
    }

    @Test
    fun `when appointmentId is valid and unconfirming, calls repository with false`() = runTest {
        coEvery { appointmentRepository.updateConfirmationStatus("appt-10", false) } returns Result.success(Unit)

        val result = useCase("appt-10", false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { appointmentRepository.updateConfirmationStatus("appt-10", false) }
    }
}
