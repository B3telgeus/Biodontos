package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.repository.AppointmentRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class GetDoctorAppointmentsUseCaseTest {

    private val appointmentRepository: AppointmentRepository = mockk()
    private lateinit var useCase: GetDoctorAppointmentsUseCase

    private val testAppointments = listOf(
        Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-123",
            startTime = LocalDateTime.of(2026, 10, 20, 8, 0),
            endTime = LocalDateTime.of(2026, 10, 20, 8, 30),
            status = AppointmentStatus.CONFIRMED,
            isConfirmed = true,
            reason = "Profilaxis"
        )
    )

    @Before
    fun setUp() {
        useCase = GetDoctorAppointmentsUseCase(appointmentRepository)
    }

    @Test
    fun `when doctorId is blank, returns failure`() = runTest {
        val result = useCase("   ")
        assertTrue(result.isFailure)
        assertEquals("El identificador del doctor no puede estar vacío", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when doctorId is valid, returns appointments list from repository`() = runTest {
        coEvery { appointmentRepository.getAppointmentsByDoctorId("doc-123") } returns Result.success(testAppointments)

        val result = useCase("doc-123")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Profilaxis", result.getOrNull()?.first()?.reason)
    }
}
