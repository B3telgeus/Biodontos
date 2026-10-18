package com.example.biodontos.presentation.auth.splash

import com.example.biodontos.MainDispatcherRule
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentSessionUseCase: GetCurrentSessionUseCase = mockk()

    @Test
    fun whenUserIsDoctor_emitsNavigateToDoctor() = runTest {
        val doctorSession = UserSession(
            id = "doc-123",
            email = "laura@biodontos.com",
            role = UserRole.DOCTOR,
            displayName = "Dra. Laura"
        )
        coEvery { getCurrentSessionUseCase() } returns doctorSession

        val viewModel = SplashViewModel(getCurrentSessionUseCase)
        val effect = viewModel.effects.first()

        assertEquals(SplashEffect.NavigateToDoctor, effect)
        assertEquals(false, viewModel.state.value.isInitializing)
    }

    @Test
    fun whenUserIsPatient_emitsNavigateToPatient() = runTest {
        val patientSession = UserSession(
            id = "pat-456",
            email = "juan@gmail.com",
            role = UserRole.PATIENT,
            displayName = "Juan Cardozo"
        )
        coEvery { getCurrentSessionUseCase() } returns patientSession

        val viewModel = SplashViewModel(getCurrentSessionUseCase)
        val effect = viewModel.effects.first()

        assertEquals(SplashEffect.NavigateToPatient, effect)
        assertEquals(false, viewModel.state.value.isInitializing)
    }

    @Test
    fun whenNoActiveSession_emitsNavigateToOnboarding() = runTest {
        coEvery { getCurrentSessionUseCase() } returns null

        val viewModel = SplashViewModel(getCurrentSessionUseCase)
        val effect = viewModel.effects.first()

        assertEquals(SplashEffect.NavigateToOnboarding, effect)
        assertEquals(false, viewModel.state.value.isInitializing)
    }

    @Test
    fun whenUserRoleIsUnknown_emitsNavigateToOnboarding() = runTest {
        val unknownSession = UserSession(
            id = "unknown-789",
            email = "guest@biodontos.com",
            role = UserRole.UNKNOWN,
            displayName = "Invitado"
        )
        coEvery { getCurrentSessionUseCase() } returns unknownSession

        val viewModel = SplashViewModel(getCurrentSessionUseCase)
        val effect = viewModel.effects.first()

        assertEquals(SplashEffect.NavigateToOnboarding, effect)
        assertEquals(false, viewModel.state.value.isInitializing)
    }
}
