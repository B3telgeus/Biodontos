package com.example.biodontos.domain.usecase.auth

import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AuthRepository

class RegisterDoctorUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        licenseNumber: String,
        specialty: String,
        phone: String? = null
    ): Result<UserSession> {
        if (firstName.isBlank()) return Result.failure(IllegalArgumentException("El nombre es obligatorio"))
        if (lastName.isBlank()) return Result.failure(IllegalArgumentException("El apellido es obligatorio"))
        if (licenseNumber.isBlank()) return Result.failure(IllegalArgumentException("La tarjeta o cédula profesional es obligatoria"))
        if (specialty.isBlank()) return Result.failure(IllegalArgumentException("La especialidad es obligatoria"))
        if (email.isBlank() || !email.contains("@")) return Result.failure(IllegalArgumentException("Ingresa un correo válido"))
        if (password.length < 6) return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres"))

        return authRepository.registerDoctor(email, password, firstName, lastName, licenseNumber, specialty, phone)
    }
}
