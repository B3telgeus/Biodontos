package com.example.biodontos.presentation.auth.reset

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.usecase.auth.ResetPasswordUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val resetPasswordUseCase: ResetPasswordUseCase = mockk()
    private val viewModel by lazy { ForgotPasswordViewModel(resetPasswordUseCase) }

    @Test
    fun `submit with invalid email sets emailError and does not call useCase`() = runTest {
        viewModel.onEmailChange("correo-invalido")
        viewModel.submit()

        val state = viewModel.state.value
        assertEquals("Ingresa un correo electrónico válido", state.emailError)
        assertFalse(state.isSubmittedSuccess)
        assertFalse(state.isLoading)
    }

    @Test
    fun `submit with valid email succeeds and updates isSubmittedSuccess`() = runTest {
        coEvery { resetPasswordUseCase("paciente@biodontos.com") } returns Result.success(Unit)

        viewModel.onEmailChange("paciente@biodontos.com")
        viewModel.submit()

        val state = viewModel.state.value
        assertNull(state.emailError)
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertTrue(state.isSubmittedSuccess)
    }

    @Test
    fun `submit when repository fails sets error message`() = runTest {
        coEvery { resetPasswordUseCase("paciente@biodontos.com") } returns Result.failure(
            Exception("No se encontró una cuenta asociada")
        )

        viewModel.onEmailChange("paciente@biodontos.com")
        viewModel.submit()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isSubmittedSuccess)
        assertEquals("No se encontró una cuenta asociada", state.error)
    }
}
