package com.example.biodontos.domain.usecase.auth

import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<UserSession> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("El correo electrónico no puede estar vacío"))
        }
        if (!email.contains("@") || !email.contains(".")) {
            return Result.failure(IllegalArgumentException("El correo electrónico no es válido"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("La contraseña no puede estar vacía"))
        }
        return authRepository.login(email, password)
    }
}
