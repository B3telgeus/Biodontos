package com.example.biodontos.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun mapsUserAlreadyRegisteredCorrectly() {
        val exception = Exception("User already registered")
        val friendlyMessage = exception.toUserFriendlyMessage()
        assertEquals("Este correo electrónico ya está registrado en el sistema.", friendlyMessage)
    }

    @Test
    fun mapsInvalidCredentialsCorrectly() {
        val exception = Exception("Invalid login credentials")
        val friendlyMessage = exception.toUserFriendlyMessage()
        assertEquals("Correo o contraseña incorrectos. Verifica tus datos.", friendlyMessage)
    }

    @Test
    fun mapsNameErrorCorrectly() {
        val exception = Exception("NameError occurred")
        val friendlyMessage = exception.toUserFriendlyMessage()
        assertEquals("Error en el nombre o formato de texto ingresado.", friendlyMessage)
    }

    @Test
    fun mapsInvalidUuidSyntaxCorrectly() {
        val exception = Exception("""ERROR: invalid input syntax for uuid: "DOC-001"""")
        val friendlyMessage = exception.toUserFriendlyMessage()
        assertEquals("Identificador de doctor o paciente con formato inválido. Verifique la sesión activa.", friendlyMessage)
    }

    @Test
    fun mapsDoctorForeignKeyErrorCorrectly() {
        val exception = Exception("""insert or update on table "informed_consents" violates foreign key constraint "informed_consents_doctor_id_fkey"""")
        val friendlyMessage = exception.toUserFriendlyMessage()
        assertEquals("El odontólogo asignado no se encuentra registrado en el sistema clínico.", friendlyMessage)
    }

    @Test
    fun mapsPeriodontalDoctorForeignKeyErrorCorrectly() {
        val exception = Exception("""insert or update on table "periodontal_examinations" violates foreign key constraint "preiodontal_examinations_doctor_id_fkey" Key is not present in table "doctors"""")
        val friendlyMessage = exception.toUserFriendlyMessage()
        assertEquals("El odontólogo responsable no se encuentra registrado en el sistema clínico. Verifique la sesión o ejecute la migración de especialistas.", friendlyMessage)
    }

    @Test
    fun returnsDefaultMessageOnNullOrUnknown() {
        val friendlyMessage = null.toUserFriendlyMessage("Por defecto")
        assertEquals("Por defecto", friendlyMessage)
    }
}
