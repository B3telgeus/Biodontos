package com.example.biodontos.presentation.patients

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PatientListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPatientsUseCase = mockk<GetPatientsUseCase>()

    @Test
    fun initLoadsPatientsSuccessfully() {
        runTest {
            val patients = listOf(Patient("1", "1010", "John", "Doe", true, false))
            coEvery { getPatientsUseCase() } returns Result.success(patients)

            val viewModel = PatientListViewModel(getPatientsUseCase)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals(patients, state.patients)
            assertNull(state.error)
        }
    }

    @Test
    fun loadPatientsHandlesError() {
        runTest {
            coEvery { getPatientsUseCase() } returns Result.failure(Exception("Error"))

            val viewModel = PatientListViewModel(getPatientsUseCase)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertTrue(state.patients.isEmpty())
            assertEquals("Error", state.error)
        }
    }
}
