package com.example.biodontos.presentation.auth.patient

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.usecase.auth.RegisterPatientAuthUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatientSignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val registerPatientAuthUseCase: RegisterPatientAuthUseCase = mockk()
    private val viewModel by lazy { PatientSignUpViewModel(registerPatientAuthUseCase) }

    @Test
    fun initialStateStartsAtStep1() {
        val state = viewModel.state.value
        assertEquals(1, state.currentStep)
        assertNull(state.error)
    }

    @Test
    fun nextStepWithInvalidOrEmptyStep1DataFailsToAdvance() {
        // Attempt next step with empty data
        viewModel.nextStep()
        val state = viewModel.state.value
        assertEquals(1, state.currentStep)
        assertNotNull(state.firstNameError)
        assertNotNull(state.lastNameError)
        assertNotNull(state.dniError)
    }

    @Test
    fun nextStepWithValidStep1DataAdvancesToStep2() {
        viewModel.onFirstNameChange("Juan")
        viewModel.onLastNameChange("Perez")
        viewModel.onDniChange("12345678")

        viewModel.nextStep()

        val state = viewModel.state.value
        assertEquals(2, state.currentStep)
        assertNull(state.firstNameError)
        assertNull(state.lastNameError)
        assertNull(state.dniError)
    }

    @Test
    fun previousStepFromStep2DecrementsBackToStep1() {
        viewModel.onFirstNameChange("Juan")
        viewModel.onLastNameChange("Perez")
        viewModel.onDniChange("12345678")
        viewModel.nextStep()
        assertEquals(2, viewModel.state.value.currentStep)

        var backToParentCalled = false
        viewModel.previousStep { backToParentCalled = true }

        assertEquals(1, viewModel.state.value.currentStep)
        assertFalse(backToParentCalled)
    }

    @Test
    fun previousStepFromStep1CallsBackToParent() {
        var backToParentCalled = false
        viewModel.previousStep { backToParentCalled = true }

        assertTrue(backToParentCalled)
    }

    @Test
    fun nextStepWithMismatchedPasswordsAtStep2FailsToAdvance() {
        viewModel.onFirstNameChange("Juan")
        viewModel.onLastNameChange("Perez")
        viewModel.onDniChange("12345678")
        viewModel.nextStep()

        viewModel.onEmailChange("juan@biodontos.com")
        viewModel.onPasswordChange("123456")
        viewModel.onConfirmPasswordChange("differentPass")

        viewModel.nextStep()

        val state = viewModel.state.value
        assertEquals(2, state.currentStep)
        assertEquals("Las contraseñas no coinciden", state.confirmPasswordError)
    }

    @Test
    fun successfulRegistrationAtStep3DispatchesSuccessEvent() = runTest {
        val session = UserSession("patient-1", "juan@biodontos.com", UserRole.PATIENT, "Juan Perez")
        coEvery {
            registerPatientAuthUseCase(
                email = "juan@biodontos.com",
                password = "password123",
                firstName = "Juan",
                lastName = "Perez",
                dniCedula = "12345678",
                phone = null
            )
        } returns Result.success(session)

        // Step 1
        viewModel.onFirstNameChange("Juan")
        viewModel.onLastNameChange("Perez")
        viewModel.onDniChange("12345678")
        viewModel.nextStep()

        // Step 2
        viewModel.onEmailChange("juan@biodontos.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.nextStep()
        assertEquals(3, viewModel.state.value.currentStep)

        // Step 3
        viewModel.onTermsAcceptedChange(true)
        viewModel.nextStep()

        val event = viewModel.events.first()
        assertTrue(event is PatientSignUpEvent.RegisteredSuccess)
        assertEquals(session, (event as PatientSignUpEvent.RegisteredSuccess).session)
    }
}
