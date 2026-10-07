package com.example.biodontos.domain.usecase.auth

import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AuthRepository

class RegisterPatientAuthUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        dniCedula: String,
        phone: String? = null
    ): Result<UserSession> {
        if (firstName.isBlank()) return Result.failure(IllegalArgumentException("El nombre es obligatorio"))
        if (lastName.isBlank()) return Result.failure(IllegalArgumentException("El apellido es obligatorio"))
        if (dniCedula.isBlank()) return Result.failure(IllegalArgumentException("El documento / DNI es obligatorio"))
        if (email.isBlank() || !email.contains("@")) return Result.failure(IllegalArgumentException("Ingresa un correo válido"))
        if (password.length < 6) return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres"))

        return authRepository.registerPatient(email, password, firstName, lastName, dniCedula, phone)
    }
}
