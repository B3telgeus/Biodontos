package com.example.biodontos.core.util

import com.example.biodontos.R
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ErrorMapperTest {

    private fun mockRestException(
        errorCode: String,
        descriptionText: String? = null,
        messageText: String? = null
    ): RestException {
        return mockk<RestException>(relaxed = true) {
            every { error } returns errorCode
            every { description } returns descriptionText
            every { message } returns (messageText ?: descriptionText ?: errorCode)
        }
    }

    @Test
    fun `toAppError maps PostgREST 23505 appointment slot collision to AppointmentSlotTaken`() {
        val ex = mockRestException(
            errorCode = "23505",
            descriptionText = "Key (doctor_id, start_time)=(doc-1, 2026-10-20 10:00:00) already exists.",
            messageText = "duplicate key value violates unique constraint \"idx_appointments_doctor_slot_unique\""
        )

        val appError = ex.toAppError()

        assertEquals(AppError.AppointmentSlotTaken, appError)
        assertEquals(R.string.error_appointment_slot_taken, appError.messageRes)
        assertEquals(
            "El horario seleccionado ya fue reservado por otro paciente. Por favor selecciona otra franja horaria.",
            ex.toUserFriendlyMessage()
        )
    }

    @Test
    fun `toAppError maps PostgREST 23505 clinical material duplicate to DuplicateMaterial`() {
        val ex = mockRestException(
            errorCode = "23505",
            descriptionText = "Key (invima_registration, batch_lot_number) already exists.",
            messageText = "duplicate key value violates unique constraint \"ux_clinical_materials_active_lot_invima\""
        )

        val appError = ex.toAppError()

        assertEquals(AppError.DuplicateMaterial, appError)
        assertEquals(R.string.error_duplicate_material, appError.messageRes)
    }

    @Test
    fun `toAppError maps PostgREST 23514 check constraint to InvalidMaterialExpirationDate`() {
        val ex = mockRestException(
            errorCode = "23514",
            messageText = "new row violates check constraint \"chk_clinical_materials_dates\""
        )

        assertEquals(AppError.InvalidMaterialExpirationDate, ex.toAppError())
    }

    @Test
    fun `toAppError maps PostgREST 23503 doctor foreign key to DoctorNotRegistered without leaking SQL`() {
        val ex = mockRestException(
            errorCode = "23503",
            descriptionText = "Key (doctor_id)=(doc-999) is not present in table \"doctors\".",
            messageText = "insert or update on table \"periodontal_examinations\" violates foreign key constraint \"periodontal_examinations_doctor_id_fkey\""
        )

        val appError = ex.toAppError()
        val message = ex.toUserFriendlyMessage()

        assertEquals(AppError.DoctorNotRegistered, appError)
        assertFalse(message.contains("periodontal_examinations", ignoreCase = true))
        assertFalse(message.contains("migración", ignoreCase = true))
        assertFalse(message.contains(".sql", ignoreCase = true))
    }

    @Test
    fun `toAppError maps PostgREST 22P02 to InvalidUuidFormat`() {
        val ex = mockRestException(
            errorCode = "22P02",
            messageText = "invalid input syntax for type uuid: \"DOC-001\""
        )

        assertEquals(AppError.InvalidUuidFormat, ex.toAppError())
        assertEquals(R.string.error_invalid_uuid_format, ex.toAppError().messageRes)
    }

    @Test
    fun `toAppError maps PostgREST PGRST200 to ServiceUnavailable without leaking migration scripts or table names`() {
        val ex = mockRestException(
            errorCode = "PGRST200",
            messageText = "Could not find the table 'public.clinical_materials' in the schema cache"
        )

        val appError = ex.toAppError()
        val friendly = ex.toUserFriendlyMessage()

        assertEquals(AppError.ServiceUnavailable, appError)
        assertEquals(R.string.error_service_unavailable, appError.messageRes)
        assertFalse(friendly.contains("clinical_materials", ignoreCase = true))
        assertFalse(friendly.contains("MIGRATION", ignoreCase = true))
        assertFalse(friendly.contains(".sql", ignoreCase = true))
    }

    @Test
    fun `toAppError maps RestException invalid_grant and UnauthorizedRestException`() {
        val invalidCreds = mockRestException(
            errorCode = "invalid_grant",
            descriptionText = "Invalid login credentials"
        )
        val forbidden = mockk<UnauthorizedRestException>(relaxed = true) {
            every { error } returns "unauthorized"
            every { description } returns "JWT expired"
            every { message } returns "JWT expired"
        }

        assertEquals(AppError.InvalidCredentials, invalidCreds.toAppError())
        assertEquals(AppError.UnauthorizedSession, forbidden.toAppError())
    }

    @Test
    fun `toAppError maps SocketTimeoutException to Timeout and IOException to NetworkConnection`() {
        val timeoutEx = SocketTimeoutException("Read timed out")
        val ioEx = IOException("Unable to resolve host")

        assertEquals(AppError.Timeout, timeoutEx.toAppError())
        assertEquals(R.string.error_network_timeout, timeoutEx.toAppError().messageRes)

        assertEquals(AppError.NetworkConnection, ioEx.toAppError())
        assertEquals(R.string.error_network_connection, ioEx.toAppError().messageRes)
    }

    @Test
    fun `legacy fallback mappings remain backwards compatible and never leak raw SQL or internal messages`() {
        assertEquals(
            "Este correo electrónico ya está registrado en el sistema.",
            Exception("User already registered").toUserFriendlyMessage()
        )
        assertEquals(
            "Correo o contraseña incorrectos. Verifica tus datos.",
            Exception("Invalid login credentials").toUserFriendlyMessage()
        )
        assertEquals(
            "Error en el nombre o formato de texto ingresado.",
            Exception("NameError occurred").toUserFriendlyMessage()
        )
        assertEquals(
            "Identificador de doctor o paciente con formato inválido. Verifique la sesión activa.",
            Exception("""ERROR: invalid input syntax for uuid: "DOC-001"""").toUserFriendlyMessage()
        )
        assertEquals(
            "El odontólogo asignado no se encuentra registrado en el sistema clínico.",
            Exception("""insert or update on table "informed_consents" violates foreign key constraint "informed_consents_doctor_id_fkey"""").toUserFriendlyMessage()
        )
        assertEquals(
            "El odontólogo asignado no se encuentra registrado en el sistema clínico.",
            Exception("""insert or update on table "periodontal_examinations" violates foreign key constraint "preiodontal_examinations_doctor_id_fkey" Key is not present in table "doctors"""").toUserFriendlyMessage()
        )

        val rawInternalFailure = RuntimeException("SELECT * FROM patients WHERE id = 'secret' FAILED at line 42")
        assertEquals(AppError.Unknown, rawInternalFailure.toAppError())
        assertEquals(
            "Ocurrió un error inesperado. Por favor, intenta de nuevo.",
            rawInternalFailure.toUserFriendlyMessage()
        )
        assertEquals("Por defecto", null.toUserFriendlyMessage("Por defecto"))
    }
}
