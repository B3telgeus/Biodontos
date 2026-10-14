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

    @Test
    fun searchQueryFiltersPatientsByNameAndDni() = runTest {
        val patient1 = Patient("1", "10203040", "Carlos", "Gomez", true, false)
        val patient2 = Patient("2", "98765432", "Maria", "Rodriguez", true, false)
        coEvery { getPatientsUseCase() } returns Result.success(listOf(patient1, patient2))

        val viewModel = PatientListViewModel(getPatientsUseCase)

        // Búsqueda por nombre
        viewModel.onSearchQueryChange("carlos")
        assertEquals(1, viewModel.state.value.filteredPatients.size)
        assertEquals("Carlos", viewModel.state.value.filteredPatients.first().firstName)

        // Búsqueda por DNI
        viewModel.onSearchQueryChange("9876")
        assertEquals(1, viewModel.state.value.filteredPatients.size)
        assertEquals("Maria", viewModel.state.value.filteredPatients.first().firstName)

        // Búsqueda sin coincidencias
        viewModel.onSearchQueryChange("99999999")
        assertTrue(viewModel.state.value.filteredPatients.isEmpty())

        // Limpiar búsqueda
        viewModel.onSearchQueryChange("")
        assertEquals(2, viewModel.state.value.filteredPatients.size)
    }
}
