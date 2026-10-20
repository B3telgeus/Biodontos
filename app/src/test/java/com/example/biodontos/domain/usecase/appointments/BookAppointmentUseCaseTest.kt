package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BookAppointmentUseCaseTest {

    private val appointmentRepository: AppointmentRepository = mockk()
    private val clinicClock: ClinicClock = mockk()

    private val testNow = LocalDateTime.of(2026, 10, 20, 10, 0)
    private lateinit var useCase: BookAppointmentUseCase

    @Before
    fun setUp() {
        useCase = BookAppointmentUseCase(appointmentRepository, clinicClock)
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns testNow
    }

    @Test
    fun `when startTime is less than 2 hours in future, returns failure`() = runTest {
        val startIn1Hour = testNow.plusHours(1)

        val result = useCase(
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = startIn1Hour
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `when startTime is valid future, creates appointment with PENDING status`() = runTest {
        val startIn3Hours = testNow.plusHours(3)
        val capturedAppointment = slot<Appointment>()

        coEvery { appointmentRepository.createAppointment(capture(capturedAppointment)) } answers {
            Result.success(capturedAppointment.captured.copy(id = "appt-new"))
        }

        val result = useCase(
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = startIn3Hours,
            reason = "Dolor molar"
        )

        assertTrue(result.isSuccess)
        val appt = capturedAppointment.captured
        assertEquals(AppointmentStatus.PENDING, appt.status)
        assertFalse(appt.isConfirmed)
        assertEquals("pat-1", appt.patientId)
        assertEquals("doc-1", appt.doctorId)
        assertEquals("Dolor molar", appt.reason)
    }
}
