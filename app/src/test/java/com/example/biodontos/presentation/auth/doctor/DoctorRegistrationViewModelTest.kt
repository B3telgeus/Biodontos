package com.example.biodontos.presentation.auth.doctor

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.usecase.auth.RegisterDoctorUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorRegistrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val registerDoctorUseCase: RegisterDoctorUseCase = mockk()
    private val viewModel by lazy { DoctorRegistrationViewModel(registerDoctorUseCase) }

    @Test
    fun initialStateStartsAtStep1() {
        val state = viewModel.state.value
        assertEquals(1, state.currentStep)
    }

    @Test
    fun nextStepWithInvalidStep1DataFailsToAdvance() {
        viewModel.nextStep()
        val state = viewModel.state.value
        assertEquals(1, state.currentStep)
        assertNotNull(state.firstNameError)
        assertNotNull(state.lastNameError)
        assertNotNull(state.emailError)
    }

    @Test
    fun nextStepWithValidStep1AdvancesToStep2() {
        viewModel.onFirstNameChange("Carlos")
        viewModel.onLastNameChange("Gomez")
        viewModel.onEmailChange("carlos@biodontos.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")

        viewModel.nextStep()

        val state = viewModel.state.value
        assertEquals(2, state.currentStep)
        assertNull(state.firstNameError)
        assertNull(state.emailError)
    }

    @Test
    fun previousStepFromStep2DecrementsToStep1() {
        viewModel.onFirstNameChange("Carlos")
        viewModel.onLastNameChange("Gomez")
        viewModel.onEmailChange("carlos@biodontos.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.nextStep()
        assertEquals(2, viewModel.state.value.currentStep)

        var backToParentCalled = false
        viewModel.previousStep { backToParentCalled = true }

        assertEquals(1, viewModel.state.value.currentStep)
        assertFalse(backToParentCalled)
    }

    @Test
    fun registerDoctorSuccessDispatchesSuccessEvent() = runTest {
        val session = UserSession("doc-1", "carlos@biodontos.com", UserRole.DOCTOR, "Carlos Gomez")
        coEvery {
            registerDoctorUseCase(
                email = "carlos@biodontos.com",
                password = "password123",
                firstName = "Carlos",
                lastName = "Gomez",
                licenseNumber = "RM-12345",
                specialty = "Ortodoncia",
                phone = null
            )
        } returns Result.success(session)

        // Step 1
        viewModel.onFirstNameChange("Carlos")
        viewModel.onLastNameChange("Gomez")
        viewModel.onEmailChange("carlos@biodontos.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.nextStep()

        // Step 2
        viewModel.onLicenseNumberChange("RM-12345")
        viewModel.onSpecialtyChange("Ortodoncia")
        viewModel.nextStep()

        val event = viewModel.events.first()
        assertTrue(event is DoctorRegistrationEvent.RegisteredSuccess)
        assertEquals(session, (event as DoctorRegistrationEvent.RegisteredSuccess).session)
    }

    @Test
    fun registerDoctorFailureUpdatesStateError() = runTest {
        coEvery {
            registerDoctorUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(Exception("User already registered"))

        // Step 1
        viewModel.onFirstNameChange("Carlos")
        viewModel.onLastNameChange("Gomez")
        viewModel.onEmailChange("carlos@biodontos.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.nextStep()

        // Step 2
        viewModel.onLicenseNumberChange("RM-12345")
        viewModel.onSpecialtyChange("Ortodoncia")
        viewModel.nextStep()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Este correo electrónico ya está registrado en el sistema.", state.error)
    }
}
