package com.example.biodontos.domain.usecase.auth

import com.example.biodontos.domain.repository.AuthRepository

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        return authRepository.resetPassword(email)
    }
}
