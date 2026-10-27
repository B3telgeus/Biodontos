package com.example.biodontos.presentation.patientportal.doctors

import app.cash.turbine.test
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.usecase.doctor.SearchDoctorsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorSearchAdversarialTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val searchDoctorsUseCase: SearchDoctorsUseCase = mockk()

    private val testDoctor1 = Doctor(
        id = DoctorId("doc-1"),
        firstName = "Carlos",
        lastName = "Mendoza",
        specialty = "Ortodoncia",
        licenseNumber = "12345",
        email = "carlos@test.com"
    )

    private val testDoctor2 = Doctor(
        id = DoctorId("doc-2"),
        firstName = "María",
        lastName = "Gómez",
        specialty = "Periodoncia",
        licenseNumber = "67890",
        email = "maria@test.com"
    )

    @Before
    fun setUp() {
        coEvery { searchDoctorsUseCase(any(), any(), any(), any()) } returns Result.success(listOf(testDoctor1, testDoctor2))
    }

    private fun createViewModel(): DoctorSearchViewModel {
        return DoctorSearchViewModel(searchDoctorsUseCase)
    }

    @Test
    fun `rapid typing within 300ms window triggers only a single search for the final query`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Clear mock invocations from init
        io.mockk.clearMocks(searchDoctorsUseCase, answers = false, recordedCalls = true)

        coEvery { searchDoctorsUseCase("Carlos", any(), any(), any()) } returns Result.success(listOf(testDoctor1))

        // Simulate rapid typing: C -> Ca -> Car -> Carl -> Carlo -> Carlos within 50ms intervals
        viewModel.onEvent(DoctorSearchUiEvent.QueryChanged("C"))
        testScheduler.advanceTimeBy(50)

        viewModel.onEvent(DoctorSearchUiEvent.QueryChanged("Ca"))
        testScheduler.advanceTimeBy(50)

        viewModel.onEvent(DoctorSearchUiEvent.QueryChanged("Car"))
        testScheduler.advanceTimeBy(50)

        viewModel.onEvent(DoctorSearchUiEvent.QueryChanged("Carl"))
        testScheduler.advanceTimeBy(50)

        viewModel.onEvent(DoctorSearchUiEvent.QueryChanged("Carlo"))
        testScheduler.advanceTimeBy(50)

        viewModel.onEvent(DoctorSearchUiEvent.QueryChanged("Carlos"))

        // Up to here, 250ms elapsed, less than 300ms debounce. No search should have been executed yet!
        coVerify(exactly = 0) {
            searchDoctorsUseCase(any(), any(), any(), any())
        }

        // Now advance past 300ms debounce window
        testScheduler.advanceTimeBy(350)
        advanceUntilIdle()

        // EMPIRICAL VERIFICATION: Exactly 1 search was fired, with "Carlos"
        coVerify(exactly = 1) {
            searchDoctorsUseCase("Carlos", any(), any(), any())
        }
        assertEquals(1, viewModel.uiState.value.doctors.size)
        assertEquals("doc-1", viewModel.uiState.value.doctors[0].id.value)
    }

    @Test
    fun `rapid specialty switching while typing honors latest query and specialty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        io.mockk.clearMocks(searchDoctorsUseCase, answers = false, recordedCalls = true)

        coEvery { searchDoctorsUseCase("Maria", "Periodoncia", any(), any()) } returns Result.success(listOf(testDoctor2))

        viewModel.onEvent(DoctorSearchUiEvent.QueryChanged("Maria"))
        viewModel.onEvent(DoctorSearchUiEvent.SpecialtySelected("Periodoncia"))
        advanceUntilIdle()

        assertEquals("Maria", viewModel.uiState.value.searchQuery)
        assertEquals("Periodoncia", viewModel.uiState.value.selectedSpecialty)
        assertEquals(1, viewModel.uiState.value.doctors.size)
        assertEquals("doc-2", viewModel.uiState.value.doctors[0].id.value)
    }
}
