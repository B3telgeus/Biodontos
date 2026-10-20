package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.DoctorRepository
import com.example.biodontos.domain.scheduling.ClinicClock
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class GetDoctorDetailUseCaseTest {

    private val doctorRepository: DoctorRepository = mockk()
    private val clinicClock: ClinicClock = mockk()

    private val testDoctor = Doctor(
        id = DoctorId("doc-1"),
        firstName = "Laura",
        lastName = "Restrepo",
        specialty = "Periodoncia",
        licenseNumber = "998877",
        email = "laura@test.com"
    )

    private val mondaySlot = WeeklyAvailability(
        id = "slot-1",
        doctorId = DoctorId("doc-1"),
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(12, 0),
        slotMinutes = 30
    )

    private lateinit var useCase: GetDoctorDetailUseCase

    @Before
    fun setUp() {
        useCase = GetDoctorDetailUseCase(doctorRepository, clinicClock)
        every { clinicClock.zone } returns ZoneId.of("America/Bogota")
        coEvery { doctorRepository.getDoctorById("doc-1") } returns Result.success(testDoctor)
        coEvery { doctorRepository.getDoctorAvailability("doc-1") } returns Result.success(listOf(mondaySlot))
    }

    @Test
    fun `when doctor exists and today is Monday at 9am, isAvailableToday is true`() = runTest {
        // Monday 2026-10-19 at 09:00 AM
        val mondayMorning = LocalDateTime.of(2026, 10, 19, 9, 0)
        every { clinicClock.now() } returns mondayMorning

        val result = useCase("doc-1")

        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()
        assertEquals(testDoctor, detail.doctor)
        assertTrue(detail.isAvailableToday)
    }

    @Test
    fun `when doctor exists but today is Tuesday, isAvailableToday is false`() = runTest {
        // Tuesday 2026-10-20 at 09:00 AM
        val tuesdayMorning = LocalDateTime.of(2026, 10, 20, 9, 0)
        every { clinicClock.now() } returns tuesdayMorning

        val result = useCase("doc-1")

        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()
        assertFalse(detail.isAvailableToday)
    }

    @Test
    fun `when doctor does not exist, returns failure`() = runTest {
        coEvery { doctorRepository.getDoctorById("doc-none") } returns Result.success(null)

        val result = useCase("doc-none")

        assertTrue(result.isFailure)
    }
}
