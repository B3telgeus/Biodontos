package com.example.biodontos.domain.usecase.appointments

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentError
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.AppointmentModificationPolicy
import com.example.biodontos.domain.scheduling.ClinicClock
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemainingAppointmentsUseCasesTest {

    private val doctorRepository: DoctorRepository = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()
    private val clinicClock: ClinicClock = mockk()
    private val modificationPolicy = AppointmentModificationPolicy(minHoursBefore = 2)

    private val testNow = LocalDateTime.of(2026, 10, 20, 8, 0) // Tuesday

    @Before
    fun setUp() {
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        every { clinicClock.now() } returns testNow
    }

    @Test
    fun `GetAvailableSlotsUseCase calculates available slots for date`() = runTest {
        val docId = DoctorId("doc-1")
        val targetDate = LocalDate.of(2026, 10, 20)
        val availability = listOf(
            WeeklyAvailability(
                id = "av-1",
                doctorId = docId,
                dayOfWeek = DayOfWeek.TUESDAY,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
                slotMinutes = 30,
                isActive = true
            )
        )
        coEvery { doctorRepository.getDoctorAvailability("doc-1") } returns Result.success(availability)
        coEvery {
            appointmentRepository.getDoctorBusySlots(
                "doc-1",
                targetDate.atStartOfDay(),
                targetDate.atTime(23, 59, 59)
            )
        } returns Result.success(emptyList())

        val useCase = GetAvailableSlotsUseCase(doctorRepository, appointmentRepository, clinicClock)
        val result = useCase(docId, targetDate)

        assertTrue(result.isSuccess)
        val slots = result.getOrDefault(emptyList())
        assertEquals(4, slots.size)
    }

    @Test
    fun `RescheduleAppointmentUseCase succeeds when valid and outside lead time window`() = runTest {
        val appt = Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = testNow.plusHours(5),
            status = AppointmentStatus.CONFIRMED
        )
        val newStart = testNow.plusDays(1)
        val newEnd = newStart.plusMinutes(30)

        coEvery { appointmentRepository.getAppointmentById("appt-1") } returns Result.success(appt)
        coEvery { appointmentRepository.rescheduleAppointment("appt-1", newStart, newEnd) } returns Result.success(Unit)

        val useCase = RescheduleAppointmentUseCase(appointmentRepository, modificationPolicy, clinicClock)
        val result = useCase("appt-1", newStart)

        assertTrue(result.isRight())
    }

    @Test
    fun `RescheduleAppointmentUseCase returns ModificationWindowClosed when newStartTime is too soon`() = runTest {
        val appt = Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-1",
            startTime = testNow.plusHours(5),
            status = AppointmentStatus.CONFIRMED
        )
        val newStartTooSoon = testNow.plusHours(1)

        coEvery { appointmentRepository.getAppointmentById("appt-1") } returns Result.success(appt)

        val useCase = RescheduleAppointmentUseCase(appointmentRepository, modificationPolicy, clinicClock)
        val result = useCase("appt-1", newStartTooSoon)

        assertTrue(result.isLeft())
        result.onLeft { err ->
            assertEquals(AppointmentError.ModificationWindowClosed(2), err)
        }
    }

    @Test
    fun `consolidated appointment use cases delegate and validate properly`() = runTest {
        val sampleAppt = Appointment(
            id = "appt-10",
            patientId = "pat-10",
            doctorId = "doc-10",
            startTime = testNow.plusDays(2),
            status = AppointmentStatus.CONFIRMED
        )

        coEvery { appointmentRepository.getAppointmentById("appt-10") } returns Result.success(sampleAppt)
        coEvery { appointmentRepository.cancelAppointment("appt-10", "Viaje") } returns Result.success(Unit)
        coEvery { appointmentRepository.updateConfirmationStatus("appt-10", true) } returns Result.success(Unit)
        coEvery { appointmentRepository.getDoctorBusySlots("doc-10", "2026-10-22") } returns Result.success(listOf("10:00:00"))
        coEvery { appointmentRepository.getAppointmentsByPatientId("pat-10") } returns Result.success(listOf(sampleAppt))

        val cancelUseCase = CancelAppointmentUseCase(appointmentRepository, modificationPolicy, clinicClock)
        assertTrue(cancelUseCase("appt-10", "Viaje").isRight())

        val confirmUseCase = ConfirmAppointmentUseCase(appointmentRepository)
        assertTrue(confirmUseCase("appt-10").isSuccess)
        assertTrue(confirmUseCase("   ").isFailure)

        val busySlotsUseCase = GetDoctorBusySlotsUseCase(appointmentRepository)
        assertEquals(1, busySlotsUseCase("doc-10", "2026-10-22").getOrDefault(emptyList()).size)
        assertTrue(busySlotsUseCase("", "2026-10-22").isFailure)

        val patientApptsUseCase = GetPatientAppointmentsUseCase(appointmentRepository)
        assertEquals(1, patientApptsUseCase("pat-10").getOrDefault(emptyList()).size)
        assertTrue(patientApptsUseCase(" ").isFailure)
    }
}
