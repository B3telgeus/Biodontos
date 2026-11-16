package com.example.biodontos.data.repository

import com.example.biodontos.data.local.SessionLocalCleaner
import com.example.biodontos.domain.model.RegistrationOutcome
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient = com.example.biodontos.core.network.SupabaseClient.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sessionLocalCleaner: SessionLocalCleaner? = null
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserSession> = withContext(ioDispatcher) {
        try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val user = supabaseClient.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("No se pudo obtener el usuario autenticado"))

            if (user.id.isBlank()) {
                return@withContext Result.failure(Exception("Identificador de usuario inválido"))
            }

            val roleStr = user.userMetadata?.get("role")?.jsonPrimitive?.content ?: "UNKNOWN"
            val role = when (roleStr.uppercase()) {
                "DOCTOR" -> UserRole.DOCTOR
                "PATIENT" -> UserRole.PATIENT
                else -> UserRole.UNKNOWN
            }
            val name = user.userMetadata?.get("first_name")?.jsonPrimitive?.content ?: ""

            Result.success(
                UserSession(
                    id = user.id,
                    email = user.email ?: email.trim(),
                    role = role,
                    displayName = name
                )
            )
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun registerPatient(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        dniCedula: String,
        phone: String?
    ): Result<RegistrationOutcome> = withContext(ioDispatcher) {
        val trimmedEmail = email.trim()
        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()
        val trimmedDni = dniCedula.trim()
        val trimmedPhone = phone?.trim()

        try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = password
                data = buildJsonObject {
                    put("role", "PATIENT")
                    put("first_name", trimmedFirstName)
                    put("last_name", trimmedLastName)
                    put("dni_cedula", trimmedDni)
                    trimmedPhone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
                }
            }

            val user = supabaseClient.auth.currentUserOrNull()

            if (user != null && user.id.isNotBlank()) {
                val session = UserSession(
                    id = user.id,
                    email = user.email ?: trimmedEmail,
                    role = UserRole.PATIENT,
                    displayName = trimmedFirstName
                )
                Result.success(RegistrationOutcome.Authenticated(session))
            } else {
                Result.success(RegistrationOutcome.PendingEmailVerification(trimmedEmail))
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun registerDoctor(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        licenseNumber: String,
        specialty: String,
        phone: String?
    ): Result<RegistrationOutcome> = withContext(ioDispatcher) {
        val trimmedEmail = email.trim()
        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()
        val trimmedLicense = licenseNumber.trim()
        val trimmedSpecialty = specialty.trim()
        val trimmedPhone = phone?.trim()

        try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = password
                data = buildJsonObject {
                    put("role", "DOCTOR")
                    put("first_name", trimmedFirstName)
                    put("last_name", trimmedLastName)
                    put("license_number", trimmedLicense)
                    put("specialty", trimmedSpecialty)
                    trimmedPhone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
                }
            }

            val user = supabaseClient.auth.currentUserOrNull()

            if (user != null && user.id.isNotBlank()) {
                val session = UserSession(
                    id = user.id,
                    email = user.email ?: trimmedEmail,
                    role = UserRole.DOCTOR,
                    displayName = "$trimmedFirstName $trimmedLastName".trim()
                )
                Result.success(RegistrationOutcome.Authenticated(session))
            } else {
                Result.success(RegistrationOutcome.PendingEmailVerification(trimmedEmail))
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            supabaseClient.auth.resetPasswordForEmail(email.trim())
            Result.success(Unit)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getCurrentSession(): UserSession? = withContext(ioDispatcher) {
        val user = supabaseClient.auth.currentUserOrNull() ?: return@withContext null
        if (user.id.isBlank()) return@withContext null

        val roleStr = user.userMetadata?.get("role")?.jsonPrimitive?.content ?: "UNKNOWN"
        val role = when (roleStr.uppercase()) {
            "DOCTOR" -> UserRole.DOCTOR
            "PATIENT" -> UserRole.PATIENT
            else -> UserRole.UNKNOWN
        }
        val name = user.userMetadata?.get("first_name")?.jsonPrimitive?.content ?: ""

        UserSession(
            id = user.id,
            email = user.email ?: "",
            role = role,
            displayName = name
        )
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            supabaseClient.auth.modifyUser(updateCurrentUser = true) {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        var remoteError: Throwable? = null
        try {
            supabaseClient.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            remoteError = e
        }

        try {
            sessionLocalCleaner?.clearSessionData()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (remoteError == null) {
                return@withContext Result.failure(e)
            }
        }

        if (remoteError != null) {
            Result.failure(remoteError)
        } else {
            Result.success(Unit)
        }
    }
}
