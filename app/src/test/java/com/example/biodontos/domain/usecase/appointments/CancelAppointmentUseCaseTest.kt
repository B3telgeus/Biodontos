package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.scheduling.AppointmentModificationPolicy
import com.example.biodontos.domain.scheduling.ClinicClock
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CancelAppointmentUseCaseTest {

    private val appointmentRepository: AppointmentRepository = mockk()
    private val modificationPolicy = AppointmentModificationPolicy(minHoursBefore = 2)
    private val clinicClock: ClinicClock = mockk()

    private val testNow = LocalDateTime.of(2026, 10, 20, 10, 0)
    private lateinit var useCase: CancelAppointmentUseCase

    @Before
    fun setUp() {
        useCase = CancelAppointmentUseCase(appointmentRepository, modificationPolicy, clinicClock)
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns testNow
    }

    @Test
    fun `when appointment is more than 2 hours away, cancellation succeeds`() = runTest {
        val appt = Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = testNow.plusHours(5),
            status = AppointmentStatus.CONFIRMED
        )
        coEvery { appointmentRepository.getAppointmentById("appt-1") } returns Result.success(appt)
        coEvery { appointmentRepository.cancelAppointment("appt-1") } returns Result.success(Unit)

        val result = useCase("appt-1")

        assertTrue(result.isRight())
    }

    @Test
    fun `when appointment is less than 2 hours away, returns ModificationWindowClosed`() = runTest {
        val appt = Appointment(
            id = "appt-soon",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = testNow.plusMinutes(90),
            status = AppointmentStatus.CONFIRMED
        )
        coEvery { appointmentRepository.getAppointmentById("appt-soon") } returns Result.success(appt)

        val result = useCase("appt-soon")

        assertTrue(result.isLeft())
        result.onLeft { error ->
            assertEquals(AppointmentError.ModificationWindowClosed(2), error)
        }
    }
}
