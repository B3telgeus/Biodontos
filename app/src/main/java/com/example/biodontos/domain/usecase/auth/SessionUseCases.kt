package com.example.biodontos.domain.usecase.auth

import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AuthRepository

class GetCurrentSessionUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): UserSession? = authRepository.getCurrentSession()
}

class LogoutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
