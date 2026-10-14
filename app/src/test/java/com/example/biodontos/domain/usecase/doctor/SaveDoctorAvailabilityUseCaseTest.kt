package com.example.biodontos.domain.usecase.doctor

import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.repository.DoctorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class SaveDoctorAvailabilityUseCaseTest {

    private val doctorRepository: DoctorRepository = mockk()
    private lateinit var useCase: SaveDoctorAvailabilityUseCase

    @Before
    fun setup() {
        useCase = SaveDoctorAvailabilityUseCase(doctorRepository)
    }

    @Test
    fun `guardar disponibilidad valida persiste correctamente`() = runTest {
        val doctorId = "doc-101"
        val availabilities = listOf(
            WeeklyAvailability(
                doctorId = DoctorId(doctorId),
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(17, 0),
                slotMinutes = 30,
                isActive = true
            )
        )

        coEvery { doctorRepository.saveDoctorAvailability(doctorId, availabilities) } returns Result.success(Unit)

        val result = useCase(doctorId, availabilities)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { doctorRepository.saveDoctorAvailability(doctorId, availabilities) }
    }

    @Test
    fun `falla si doctorId esta vacio`() = runTest {
        val result = useCase("", emptyList())

        assertTrue(result.isFailure)
        assertEquals("El ID del doctor no puede estar vacío", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { doctorRepository.saveDoctorAvailability(any(), any()) }
    }

    @Test
    fun `falla si hora fin es anterior a hora inicio en dia activo`() = runTest {
        val doctorId = "doc-101"
        val exception = runCatching {
            listOf(
                WeeklyAvailability(
                    doctorId = DoctorId(doctorId),
                    dayOfWeek = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(17, 0),
                    endTime = LocalTime.of(8, 0),
                    slotMinutes = 30,
                    isActive = true
                )
            )
        }.exceptionOrNull()

        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        coVerify(exactly = 0) { doctorRepository.saveDoctorAvailability(any(), any()) }
    }

    @Test
    fun `falla si slotMinutes esta fuera de rango`() = runTest {
        val doctorId = "doc-101"
        val invalidSlot = runCatching {
            WeeklyAvailability(
                doctorId = DoctorId(doctorId),
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(12, 0),
                slotMinutes = 5, // Inválido (< 10)
                isActive = true
            )
        }

        assertTrue(invalidSlot.isFailure)
        coVerify(exactly = 0) { doctorRepository.saveDoctorAvailability(any(), any()) }
    }

    @Test
    fun `propaga error si el repositorio falla al guardar`() = runTest {
        val doctorId = "doc-101"
        val availabilities = listOf(
            WeeklyAvailability(
                doctorId = DoctorId(doctorId),
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(17, 0),
                slotMinutes = 30,
                isActive = true
            )
        )
        val expectedError = RuntimeException("Error en base de datos")
        coEvery { doctorRepository.saveDoctorAvailability(doctorId, availabilities) } returns Result.failure(expectedError)

        val result = useCase(doctorId, availabilities)

        assertTrue(result.isFailure)
        assertEquals(expectedError, result.exceptionOrNull())
    }
}
