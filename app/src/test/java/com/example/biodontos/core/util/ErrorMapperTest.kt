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
    fun returnsDefaultMessageOnNullOrUnknown() {
        val friendlyMessage = null.toUserFriendlyMessage("Por defecto")
        assertEquals("Por defecto", friendlyMessage)
    }
}
