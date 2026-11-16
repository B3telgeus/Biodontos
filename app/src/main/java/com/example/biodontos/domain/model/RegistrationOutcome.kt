package com.example.biodontos.domain.model

sealed interface RegistrationOutcome {
    data class Authenticated(val session: UserSession) : RegistrationOutcome
    data class PendingEmailVerification(val email: String) : RegistrationOutcome
}
