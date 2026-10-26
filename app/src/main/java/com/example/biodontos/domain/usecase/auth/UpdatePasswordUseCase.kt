package com.example.biodontos.domain.usecase.auth

import com.example.biodontos.domain.repository.AuthRepository

class UpdatePasswordUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(newPassword: String): Result<Unit> {
        if (newPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("La contraseña no puede estar vacía"))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres"))
        }
        return authRepository.updatePassword(newPassword)
    }
}
