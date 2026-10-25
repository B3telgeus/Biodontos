package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.DoctorRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class RemainingDoctorUseCasesTest {

    private val doctorRepository: DoctorRepository = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()

    @Test
    fun `GetDoctorAvailabilityUseCase validates doctorId and fetches availability`() = runTest {
        val useCase = GetDoctorAvailabilityUseCase(doctorRepository)

        assertTrue(useCase("").isFailure)

        val avail = WeeklyAvailability(
            id = "av-1",
            doctorId = DoctorId("doc-1"),
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0)
        )
        coEvery { doctorRepository.getDoctorAvailability("doc-1") } returns Result.success(listOf(avail))

        val result = useCase("doc-1")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrDefault(emptyList()).size)
    }

    @Test
    fun `GetMyDoctorsUseCase filters doctors by patient appointments`() = runTest {
        val doc1 = Doctor(id = DoctorId("doc-1"), firstName = "Ana", lastName = "López", specialty = "Endodoncia")
        val doc2 = Doctor(id = DoctorId("doc-2"), firstName = "Luis", lastName = "Pérez", specialty = "Ortodoncia")
        val appt = Appointment(
            id = "appt-1",
            patientId = "pat-1",
            doctorId = "doc-2",
            startTime = LocalDateTime.of(2026, 10, 20, 10, 0)
        )

        coEvery { doctorRepository.getDoctors() } returns Result.success(listOf(doc1, doc2))
        coEvery { appointmentRepository.getAppointmentsByPatientId("pat-1") } returns Result.success(listOf(appt))

        val useCase = GetMyDoctorsUseCase(doctorRepository, appointmentRepository)
        val result = useCase("pat-1")

        assertTrue(result.isSuccess)
        val doctors = result.getOrDefault(emptyList())
        assertEquals(1, doctors.size)
        assertEquals("doc-2", doctors.first().id.value)
    }

    @Test
    fun `SearchDoctorsUseCase normalizes query and specialty filter`() = runTest {
        val doc = Doctor(id = DoctorId("doc-1"), firstName = "Carlos", lastName = "Mendoza", specialty = "Ortodoncia")
        coEvery {
            doctorRepository.searchDoctors(query = "Carlos", specialty = null, limit = 20, offset = 0)
        } returns Result.success(listOf(doc))

        val useCase = SearchDoctorsUseCase(doctorRepository)
        val result = useCase(query = "  Carlos  ", specialty = "Todas")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrDefault(emptyList()).size)
    }
}
