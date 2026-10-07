package com.example.biodontos.presentation.auth.login

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.usecase.auth.LoginUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loginUseCase: LoginUseCase = mockk()
    private val viewModel by lazy { LoginViewModel(loginUseCase) }

    @Test
    fun initialStateIsDefault() {
        val state = viewModel.state.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun loginWithValidCredentialsDispatchesLoginSuccessEvent() = runTest {
        val session = UserSession("1", "user@biodontos.com", UserRole.DOCTOR, "Doctor Test")
        coEvery { loginUseCase("user@biodontos.com", "pass123") } returns Result.success(session)

        viewModel.onEmailChange("user@biodontos.com")
        viewModel.onPasswordChange("pass123")
        viewModel.login()

        val event = viewModel.events.first()
        assertTrue(event is LoginEvent.LoginSuccess)
        assertEquals(session, (event as LoginEvent.LoginSuccess).session)
    }

    @Test
    fun loginWithInvalidCredentialsSetsErrorState() = runTest {
        coEvery { loginUseCase("user@biodontos.com", "wrong") } returns Result.failure(Exception("Credenciales inválidas"))

        viewModel.onEmailChange("user@biodontos.com")
        viewModel.onPasswordChange("wrong")
        viewModel.login()

        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.error)
        assertEquals("Credenciales inválidas", state.error)
    }
}
