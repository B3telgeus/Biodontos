package com.example.biodontos.presentation.patientportal.doctors

import app.cash.turbine.test
import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Doctor
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.usecase.doctor.SearchDoctorsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchDoctorsUseCase: SearchDoctorsUseCase = mockk()

    private val testDoctors = listOf(
        Doctor(
            id = DoctorId("doc-1"),
            firstName = "Carlos",
            lastName = "Mendoza",
            specialty = "Ortodoncia",
            licenseNumber = "12345",
            email = "carlos@test.com"
        ),
        Doctor(
            id = DoctorId("doc-2"),
            firstName = "María",
            lastName = "Gómez",
            specialty = "Periodoncia",
            licenseNumber = "67890",
            email = "maria@test.com"
        )
    )

    @Before
    fun setUp() {
        coEvery { searchDoctorsUseCase(any(), any(), any(), any()) } returns Result.success(testDoctors)
    }

    private fun createViewModel(): DoctorSearchViewModel {
        return DoctorSearchViewModel(searchDoctorsUseCase)
    }

    @Test
    fun `initial load populates doctors in uiState`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.doctors.size)
        assertEquals("Dr. Carlos Mendoza", state.doctors[0].displayName)
    }

    @Test
    fun `query change triggers debounced search`() = runTest {
        coEvery { searchDoctorsUseCase("Carlos", any(), any(), any()) } returns Result.success(listOf(testDoctors[0]))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DoctorSearchUiEvent.QueryChanged("Carlos"))
        assertEquals("Carlos", viewModel.uiState.value.searchQuery)

        advanceTimeBy(350L)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.doctors.size)
        assertEquals("doc-1", viewModel.uiState.value.doctors[0].id.value)
    }

    @Test
    fun `specialty selection triggers immediate reload`() = runTest {
        coEvery { searchDoctorsUseCase(any(), "Periodoncia", any(), any()) } returns Result.success(listOf(testDoctors[1]))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DoctorSearchUiEvent.SpecialtySelected("Periodoncia"))
        advanceUntilIdle()

        assertEquals("Periodoncia", viewModel.uiState.value.selectedSpecialty)
        assertEquals(1, viewModel.uiState.value.doctors.size)
        assertEquals("Periodoncia", viewModel.uiState.value.doctors[0].specialty)
    }

    @Test
    fun `doctor selected emits NavigateToDetail effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(DoctorSearchUiEvent.DoctorSelected("doc-1"))
            val effect = awaitItem()
            assertEquals(DoctorSearchUiEffect.NavigateToDetail("doc-1"), effect)
        }
    }

    @Test
    fun `book doctor emits NavigateToBooking effect`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onEvent(DoctorSearchUiEvent.BookDoctor("doc-2"))
            val effect = awaitItem()
            assertEquals(DoctorSearchUiEffect.NavigateToBooking("doc-2"), effect)
        }
    }
}
