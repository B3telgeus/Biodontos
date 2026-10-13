package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient
import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import com.example.biodontos.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AuthRepositoryImpl : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val user = SupabaseClient.client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("No se pudo obtener el usuario autenticado"))

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
                    email = user.email ?: email,
                    role = role,
                    displayName = name
                )
            )
        } catch (e: Exception) {
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
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    put("role", "PATIENT")
                    put("first_name", firstName.trim())
                    put("last_name", lastName.trim())
                    put("dni_cedula", dniCedula.trim())
                    phone?.let { put("phone", it.trim()) }
                }
            }

            val user = SupabaseClient.client.auth.currentUserOrNull()

            // Guardar o sincronizar también en la tabla patients de la BD
            try {
                val patientId = user?.id ?: Patient.generateIdFromDni(dniCedula.trim())
                SupabaseClient.client.from("patients").upsert(
                    PatientDto(
                        id = patientId,
                        userId = user?.id,
                        dniCedula = dniCedula.trim(),
                        firstName = firstName.trim(),
                        firstLastName = lastName.trim(),
                        phone = phone?.trim().orEmpty(),
                        email = email.trim(),
                        criticalAllergies = "Ninguna",
                        consentSigned = true
                    )
                )
            } catch (_: Exception) {
                // Silencioso si la tabla tiene RLS o si el usuario aún requiere confirmación por email
            }

            val session = UserSession(
                id = user?.id ?: "",
                email = email.trim(),
                role = UserRole.PATIENT,
                displayName = firstName.trim()
            )
            Result.success(session)
        } catch (e: Exception) {
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
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    put("role", "DOCTOR")
                    put("first_name", firstName.trim())
                    put("last_name", lastName.trim())
                    put("license_number", licenseNumber.trim())
                    put("specialty", specialty.trim())
                    phone?.let { put("phone", it.trim()) }
                }
            }

            val user = SupabaseClient.client.auth.currentUserOrNull()

            try {
                SupabaseClient.client.from("doctors").insert(
                    buildJsonObject {
                        user?.id?.let { put("user_id", it) }
                        put("first_name", firstName.trim())
                        put("last_name", lastName.trim())
                        put("email", email.trim())
                        put("license_number", licenseNumber.trim())
                        put("specialty", specialty.trim())
                        phone?.let { put("phone", it.trim()) }
                    }
                )
            } catch (_: Exception) {
                // Silencioso si ya está manejado por trigger o RLS
            }

            val session = UserSession(
                id = user?.id ?: "",
                email = email.trim(),
                role = UserRole.DOCTOR,
                displayName = "$firstName $lastName".trim()
            )
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.auth.resetPasswordForEmail(email.trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentSession(): UserSession? = withContext(Dispatchers.IO) {
        val user = SupabaseClient.client.auth.currentUserOrNull() ?: return@withContext null
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

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
