package com.example.biodontos.presentation.auth.splash

import androidx.compose.runtime.Immutable

@Immutable
data class SplashState(
    val isInitializing: Boolean = true
)

sealed interface SplashEffect {
    data object NavigateToDoctor : SplashEffect
    data object NavigateToPatient : SplashEffect
    data object NavigateToOnboarding : SplashEffect
}
