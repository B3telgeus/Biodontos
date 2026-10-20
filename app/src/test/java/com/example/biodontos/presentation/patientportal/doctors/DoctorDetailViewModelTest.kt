package com.example.biodontos.presentation.patientportal.doctors

import app.cash.turbine.test
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorDetail
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.WeeklyAvailability
import com.example.biodontos.domain.usecase.doctor.GetDoctorDetailUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getDoctorDetailUseCase: GetDoctorDetailUseCase = mockk()

    private val testDoctor = Doctor(
        id = DoctorId("doc-1"),
        firstName = "Andrés",
        lastName = "Silva",
        specialty = "Ortodoncia",
        licenseNumber = "778899",
        email = "andres@test.com",
        bio = "Especialista en ortodoncia invisible y brackets estéticos"
    )

    private val testDetail = DoctorDetail(
        doctor = testDoctor,
        availability = listOf(
            WeeklyAvailability(
                id = "avail-1",
                doctorId = DoctorId("doc-1"),
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(12, 0),
                slotMinutes = 30
            )
        ),
        isAvailableToday = true
    )

    @Before
    fun setUp() {
        coEvery { getDoctorDetailUseCase("doc-1") } returns Result.success(testDetail)
    }

    private fun createViewModel(doctorId: String = "doc-1"): DoctorDetailViewModel {
        return DoctorDetailViewModel(doctorId, getDoctorDetailUseCase)
    }

    @Test
    fun `initial load populates doctor detail successfully`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.doctorDetail)
        assertEquals("Dr. Andrés Silva", state.doctorDetail?.doctor?.displayName)
        assertTrue(state.doctorDetail?.isAvailableToday == true)
        assertEquals(1, state.doctorDetail?.availability?.size)
    }

    @Test
    fun `initial load failure sets error message`() = runTest {
        coEvery { getDoctorDetailUseCase("doc-unknown") } returns Result.failure(Exception("Doctor no encontrado"))

        val viewModel = createViewModel("doc-unknown")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Doctor no encontrado", state.errorMessage)
    }

    @Test
    fun `book appointment event emits NavigateToBooking effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(DoctorDetailUiEvent.BookAppointment)
            val effect = awaitItem()
            assertEquals(DoctorDetailUiEffect.NavigateToBooking("doc-1"), effect)
        }
    }
}
