package com.example.biodontos.data.repository

import com.example.biodontos.data.dto.DoctorAvailabilityDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorRepositoryImplTest {

    private val supabaseClient: SupabaseClient = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DoctorRepositoryImpl

    @Before
    fun setUp() {
        repository = DoctorRepositoryImpl(
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `getDoctorAvailability returns emptyList when doctor has no configured schedules`() = runTest(testDispatcher) {
        repository.availabilityQuery = { emptyList() }

        val result = repository.getDoctorAvailability("doc-empty")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrNull())
    }

    @Test
    fun `getDoctorAvailability maps configured records to WeeklyAvailability`() = runTest(testDispatcher) {
        repository.availabilityQuery = {
            listOf(
                DoctorAvailabilityDto(
                    id = "avail-1",
                    doctorId = "doc-1",
                    dayOfWeek = 1,
                    startTime = "08:00:00",
                    endTime = "12:00:00",
                    slotMinutes = 30,
                    isActive = true
                )
            )
        }

        val result = repository.getDoctorAvailability("doc-1")

        assertTrue(result.isSuccess)
        val availabilities = result.getOrNull().orEmpty()
        assertEquals(1, availabilities.size)
        assertEquals(DayOfWeek.MONDAY, availabilities[0].dayOfWeek)
        assertEquals(LocalTime.of(8, 0), availabilities[0].startTime)
        assertEquals(LocalTime.of(12, 0), availabilities[0].endTime)
    }

    @Test
    fun `getDoctorAvailability propagates network and PostgREST exceptions as Result failure without synthetic schedules`() = runTest(testDispatcher) {
        val postgrestError = mockk<RestException>(relaxed = true) {
            every { error } returns "PGRST200"
            every { message } returns "Could not find the table 'public.doctor_availability' in the schema cache"
        }
        repository.availabilityQuery = { throw postgrestError }

        val result = repository.getDoctorAvailability("doc-1")

        assertTrue(result.isFailure)
        assertEquals(postgrestError, result.exceptionOrNull())
    }

    @Test
    fun `getDoctorAvailability propagates IOException as Result failure`() = runTest(testDispatcher) {
        val ioError = IOException("Network unreachable")
        repository.availabilityQuery = { throw ioError }

        val result = repository.getDoctorAvailability("doc-1")

        assertTrue(result.isFailure)
        assertEquals(ioError, result.exceptionOrNull())
    }

    @Test
    fun `getDoctorAvailability rethrows CancellationException`() = runTest(testDispatcher) {
        repository.availabilityQuery = { throw CancellationException("Cancelled") }

        try {
            repository.getDoctorAvailability("doc-1")
            fail("Expected CancellationException to be rethrown")
        } catch (e: CancellationException) {
            assertEquals("Cancelled", e.message)
        }
    }
}
