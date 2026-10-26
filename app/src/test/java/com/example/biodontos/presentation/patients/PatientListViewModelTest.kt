package com.example.biodontos.presentation.patients

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun initLoadsPatientsSuccessfully() = runTest {
        val patients = listOf(
            Patient("1", "1010", "John", "Doe", consentSigned = true, hasCriticalAllergies = false)
        )
        coEvery { getPatientsUseCase() } returns Result.success(patients)

        val viewModel = PatientListViewModel(getPatientsUseCase)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals(patients, state.patients)
        assertNull(state.error)
        assertEquals(1, state.totalCount)
        assertEquals(1, state.filteredPatients.size)
    }

    @Test
    fun loadPatientsHandlesError() = runTest {
        coEvery { getPatientsUseCase() } returns Result.failure(Exception("Error al consultar pacientes"))

        val viewModel = PatientListViewModel(getPatientsUseCase)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.patients.isEmpty())
        assertEquals("Error al consultar pacientes", state.error)
    }

    @Test
    fun searchQueryFiltersPatientsByNameAndDni() = runTest {
        val patient1 = Patient("1", "10203040", "Carlos", "Gomez", consentSigned = true, hasCriticalAllergies = false)
        val patient2 = Patient("2", "98765432", "Maria", "Rodriguez", consentSigned = true, hasCriticalAllergies = false)
        coEvery { getPatientsUseCase() } returns Result.success(listOf(patient1, patient2))

        val viewModel = PatientListViewModel(getPatientsUseCase)
        advanceUntilIdle()

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

    @Test
    fun `when filter is selected, filteredPatients reflects quick filter criteria`() = runTest {
        val patient1 = Patient("1", "1020", "Ana", "Ruiz", consentSigned = true, hasCriticalAllergies = true, allergiesDescription = "Penicilina")
        val patient2 = Patient("2", "2030", "Bernardo", "Soto", consentSigned = false, hasCriticalAllergies = false)
        val patient3 = Patient("3", "3040", "Camila", "Torres", consentSigned = true, hasCriticalAllergies = false)
        coEvery { getPatientsUseCase() } returns Result.success(listOf(patient1, patient2, patient3))

        val viewModel = PatientListViewModel(getPatientsUseCase)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.totalCount)
        assertEquals(1, state.criticalAllergiesCount)
        assertEquals(1, state.pendingConsentCount)

        // Filtro por defecto ALL
        assertEquals(PatientQuickFilter.ALL, state.selectedFilter)
        assertEquals(3, state.filteredPatients.size)

        // Filtro CRITICAL_ALLERGIES
        viewModel.onFilterSelected(PatientQuickFilter.CRITICAL_ALLERGIES)
        assertEquals(1, viewModel.state.value.filteredPatients.size)
        assertEquals("Ana", viewModel.state.value.filteredPatients.first().firstName)

        // Filtro PENDING_CONSENT
        viewModel.onFilterSelected(PatientQuickFilter.PENDING_CONSENT)
        assertEquals(1, viewModel.state.value.filteredPatients.size)
        assertEquals("Bernardo", viewModel.state.value.filteredPatients.first().firstName)

        // Volver a ALL
        viewModel.onFilterSelected(PatientQuickFilter.ALL)
        assertEquals(3, viewModel.state.value.filteredPatients.size)
    }

    @Test
    fun `when search query and filter are combined, results intersect correctly`() = runTest {
        val patient1 = Patient("1", "1020", "Ana", "Ruiz", consentSigned = false, hasCriticalAllergies = false)
        val patient2 = Patient("2", "2030", "Bernardo", "Ruiz", consentSigned = true, hasCriticalAllergies = false)
        val patient3 = Patient("3", "3040", "Carlos", "Pérez", consentSigned = false, hasCriticalAllergies = false)
        coEvery { getPatientsUseCase() } returns Result.success(listOf(patient1, patient2, patient3))

        val viewModel = PatientListViewModel(getPatientsUseCase)
        advanceUntilIdle()

        // Filtrar por apellido "Ruiz"
        viewModel.onSearchQueryChange("ruiz")
        assertEquals(2, viewModel.state.value.filteredPatients.size)

        // Activar filtro PENDING_CONSENT: solo Ana Ruiz cumple ambas condiciones
        viewModel.onFilterSelected(PatientQuickFilter.PENDING_CONSENT)
        assertEquals(1, viewModel.state.value.filteredPatients.size)
        assertEquals("Ana", viewModel.state.value.filteredPatients.first().firstName)

        // Buscar texto que no coincide con Ana
        viewModel.onSearchQueryChange("carlos")
        // Carlos tiene consentimiento pendiente, coincide
        assertEquals(1, viewModel.state.value.filteredPatients.size)
        assertEquals("Carlos", viewModel.state.value.filteredPatients.first().firstName)
    }

    @Test
    fun `when refresh is called, reloads patients and updates isRefreshing`() = runTest {
        val initialPatients = listOf(Patient("1", "1020", "Ana", "Ruiz", consentSigned = true, hasCriticalAllergies = false))
        val refreshedPatients = listOf(
            Patient("1", "1020", "Ana", "Ruiz", consentSigned = true, hasCriticalAllergies = false),
            Patient("2", "2030", "Bernardo", "Soto", consentSigned = false, hasCriticalAllergies = false)
        )

        coEvery { getPatientsUseCase() } returns Result.success(initialPatients) andThen Result.success(refreshedPatients)

        val viewModel = PatientListViewModel(getPatientsUseCase)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.patients.size)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.patients.size)
        assertFalse(viewModel.state.value.isRefreshing)
        assertNull(viewModel.state.value.error)
        coVerify(exactly = 2) { getPatientsUseCase() }
    }
}
