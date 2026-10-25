package com.example.biodontos.presentation.auth.reset

import androidx.compose.runtime.Immutable

@Immutable
data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSubmittedSuccess: Boolean = false,
    val error: String? = null,
    val emailError: String? = null
)
