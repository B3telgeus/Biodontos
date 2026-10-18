package com.example.biodontos.presentation.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.usecase.auth.GetCurrentSessionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state = _state.asStateFlow()

    private val _effects = Channel<SplashEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            // Breve espera estética (1200ms) para mostrar la animación del logo y el branding institucional
            val startTime = System.currentTimeMillis()
            val session = try {
                getCurrentSessionUseCase()
            } catch (_: Exception) {
                null
            }

            val elapsed = System.currentTimeMillis() - startTime
            val remainingDelay = (1200L - elapsed).coerceAtLeast(0L)
            if (remainingDelay > 0) {
                delay(remainingDelay)
            }

            _state.value = SplashState(isInitializing = false)

            if (session != null) {
                when (session.role) {
                    UserRole.DOCTOR -> _effects.send(SplashEffect.NavigateToDoctor)
                    UserRole.PATIENT -> _effects.send(SplashEffect.NavigateToPatient)
                    UserRole.UNKNOWN -> _effects.send(SplashEffect.NavigateToOnboarding)
                }
            } else {
                _effects.send(SplashEffect.NavigateToOnboarding)
            }
        }
    }
}
