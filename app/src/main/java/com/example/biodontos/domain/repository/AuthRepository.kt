package com.example.biodontos.domain.repository

import com.example.biodontos.domain.model.RegistrationOutcome
import com.example.biodontos.domain.model.UserSession

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<UserSession>

    suspend fun registerPatient(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        dniCedula: String,
        phone: String? = null
    ): Result<RegistrationOutcome>

    suspend fun registerDoctor(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        licenseNumber: String,
        specialty: String,
        phone: String? = null
    ): Result<RegistrationOutcome>

    suspend fun resetPassword(email: String): Result<Unit>

    suspend fun updatePassword(newPassword: String): Result<Unit>

    suspend fun getCurrentSession(): UserSession?

    suspend fun logout(): Result<Unit>
}
