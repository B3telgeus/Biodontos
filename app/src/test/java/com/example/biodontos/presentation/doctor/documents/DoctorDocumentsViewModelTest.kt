package com.example.biodontos.presentation.doctor.documents

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.usecase.patients.GetPatientsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorDocumentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getPatientsUseCase = mockk<GetPatientsUseCase>()

    private val patient1 = Patient(
        id = "1",
        dniCedula = "10203040",
        firstName = "Carlos",
        firstLastName = "Perez",
        consentSigned = true,
        hasCriticalAllergies = false
    )

    private val patient2 = Patient(
        id = "2",
        dniCedula = "98765432",
        firstName = "Maria",
        firstLastName = "Rodriguez",
        consentSigned = false,
        hasCriticalAllergies = true,
        allergiesDescription = "Penicilina"
    )

    private val patient3 = Patient(
        id = "3",
        dniCedula = "11223344",
        firstName = "Ana",
        firstLastName = "Gomez",
        consentSigned = true,
        hasCriticalAllergies = true,
        allergiesDescription = "Látex"
    )

    @Test
    fun `initialization loads patients and computes KPI metrics accurately`() = runTest {
        val patients = listOf(patient1, patient2, patient3)
        coEvery { getPatientsUseCase() } returns Result.success(patients)

        val viewModel = DoctorDocumentsViewModel(getPatientsUseCase)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(3, state.patients.size)
        assertEquals(3, state.totalRecords)
        assertEquals(2, state.consentedCount)
        assertEquals(1, state.pendingConsentCount)
        assertEquals(2, state.criticalAllergiesCount)
        assertEquals(3, state.filteredPatients.size)
    }

    @Test
    fun `initialization failure updates state with error message`() = runTest {
        coEvery { getPatientsUseCase() } returns Result.failure(RuntimeException("Error al conectar"))

        val viewModel = DoctorDocumentsViewModel(getPatientsUseCase)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Error al conectar", state.errorMessage)
        assertTrue(state.patients.isEmpty())
        assertEquals(0, state.totalRecords)
    }

    @Test
    fun `search query filters by patient name and DNI`() = runTest {
        val patients = listOf(patient1, patient2, patient3)
        coEvery { getPatientsUseCase() } returns Result.success(patients)

        val viewModel = DoctorDocumentsViewModel(getPatientsUseCase)

        // Búsqueda por nombre
        viewModel.onEvent(DoctorDocumentsUiEvent.SearchQueryChanged("carlos"))
        assertEquals(1, viewModel.uiState.value.filteredPatients.size)
        assertEquals("Carlos", viewModel.uiState.value.filteredPatients.first().firstName)

        // Búsqueda por DNI
        viewModel.onEvent(DoctorDocumentsUiEvent.SearchQueryChanged("9876"))
        assertEquals(1, viewModel.uiState.value.filteredPatients.size)
        assertEquals("Maria", viewModel.uiState.value.filteredPatients.first().firstName)

        // Búsqueda sin coincidencias
        viewModel.onEvent(DoctorDocumentsUiEvent.SearchQueryChanged("99999999"))
        assertTrue(viewModel.uiState.value.filteredPatients.isEmpty())

        // Limpiar búsqueda
        viewModel.onEvent(DoctorDocumentsUiEvent.SearchQueryChanged(""))
        assertEquals(3, viewModel.uiState.value.filteredPatients.size)
    }

    @Test
    fun `filter chips filter by clinical status correctly`() = runTest {
        val patients = listOf(patient1, patient2, patient3)
        coEvery { getPatientsUseCase() } returns Result.success(patients)

        val viewModel = DoctorDocumentsViewModel(getPatientsUseCase)

        // Consentimiento Firmado
        viewModel.onEvent(DoctorDocumentsUiEvent.FilterSelected(DocumentFilter.CONSENT_SIGNED))
        assertEquals(2, viewModel.uiState.value.filteredPatients.size)
        assertTrue(viewModel.uiState.value.filteredPatients.all { it.consentSigned })

        // Firma Pendiente
        viewModel.onEvent(DoctorDocumentsUiEvent.FilterSelected(DocumentFilter.CONSENT_PENDING))
        assertEquals(1, viewModel.uiState.value.filteredPatients.size)
        assertEquals("Maria", viewModel.uiState.value.filteredPatients.first().firstName)

        // Alergias Críticas
        viewModel.onEvent(DoctorDocumentsUiEvent.FilterSelected(DocumentFilter.CRITICAL_ALLERGIES))
        assertEquals(2, viewModel.uiState.value.filteredPatients.size)
        assertTrue(viewModel.uiState.value.filteredPatients.all { it.hasCriticalAllergies })

        // Todos
        viewModel.onEvent(DoctorDocumentsUiEvent.FilterSelected(DocumentFilter.ALL))
        assertEquals(3, viewModel.uiState.value.filteredPatients.size)
    }

    @Test
    fun `search query and filter chip combine predictably`() = runTest {
        val patients = listOf(patient1, patient2, patient3)
        coEvery { getPatientsUseCase() } returns Result.success(patients)

        val viewModel = DoctorDocumentsViewModel(getPatientsUseCase)

        // Filtro Alergias Críticas + Búsqueda "Ana"
        viewModel.onEvent(DoctorDocumentsUiEvent.FilterSelected(DocumentFilter.CRITICAL_ALLERGIES))
        viewModel.onEvent(DoctorDocumentsUiEvent.SearchQueryChanged("Ana"))
        assertEquals(1, viewModel.uiState.value.filteredPatients.size)
        assertEquals("Ana", viewModel.uiState.value.filteredPatients.first().firstName)

        // Filtro Alergias Críticas + Búsqueda "Carlos" (no tiene alergia)
        viewModel.onEvent(DoctorDocumentsUiEvent.SearchQueryChanged("Carlos"))
        assertTrue(viewModel.uiState.value.filteredPatients.isEmpty())
    }

    @Test
    fun `refresh event triggers reload`() = runTest {
        coEvery { getPatientsUseCase() } returns Result.success(listOf(patient1))

        val viewModel = DoctorDocumentsViewModel(getPatientsUseCase)
        viewModel.onEvent(DoctorDocumentsUiEvent.Refresh)

        coVerify(atLeast = 2) { getPatientsUseCase() }
    }

    @Test
    fun `refresh recovers from previous failure`() = runTest {
        coEvery { getPatientsUseCase() } returns Result.failure(RuntimeException("Error inicial"))

        val viewModel = DoctorDocumentsViewModel(getPatientsUseCase)
        assertEquals("Error inicial", viewModel.uiState.value.errorMessage)

        coEvery { getPatientsUseCase() } returns Result.success(listOf(patient1, patient2))
        viewModel.onEvent(DoctorDocumentsUiEvent.Refresh)

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
        assertEquals(2, state.patients.size)
    }
}
