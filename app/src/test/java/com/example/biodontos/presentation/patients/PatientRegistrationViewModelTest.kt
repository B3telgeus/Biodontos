package com.example.biodontos.presentation.patients

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.usecase.patients.SavePatientUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PatientRegistrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savePatientUseCase = mockk<SavePatientUseCase>()

    @Test
    fun savePatientWithEmptyFieldsShowsErrors() {
        runTest {
            val viewModel = PatientRegistrationViewModel(savePatientUseCase)

            viewModel.savePatient()

            val state = viewModel.state.value
            assertEquals("El DNI es obligatorio", state.dniError)
            assertEquals("El nombre es obligatorio", state.firstNameError)
            assertEquals("Los apellidos son obligatorios", state.lastNameError)
        }
    }

    @Test
    fun savePatientSuccessfullySendsEvent() {
        runTest {
            coEvery { savePatientUseCase(any()) } returns Result.success(Unit)
            val viewModel = PatientRegistrationViewModel(savePatientUseCase)

            viewModel.onDniChange("123")
            viewModel.onFirstNameChange("John")
            viewModel.onLastNameChange("Doe")
            
            viewModel.savePatient()

            val event = viewModel.events.first()
            assertEquals(PatientRegistrationEvent.SavedSuccessfully, event)
            assertEquals(true, viewModel.state.value.isSaved)
        }
    }
}
