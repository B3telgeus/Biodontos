package com.example.biodontos.core.util

fun Throwable?.toUserFriendlyMessage(defaultMessage: String = "Ocurrió un error inesperado. Por favor, intenta de nuevo."): String {
    val message = this?.message?.lowercase() ?: return defaultMessage
    return when {
        message.contains("user already registered") || message.contains("already registered") -> 
            "Este correo electrónico ya está registrado en el sistema."
        message.contains("invalid login credentials") || message.contains("invalid credentials") || message.contains("invalid grant") -> 
            "Correo o contraseña incorrectos. Verifica tus datos."
        message.contains("password should be at least") || message.contains("weak password") -> 
            "La contraseña es demasiado débil o no cumple con los requisitos mínimos."
        message.contains("nameerror") || message.contains("name error") -> 
            "Error en el nombre o formato de texto ingresado."
        message.contains("network") || message.contains("connect") || message.contains("timeout") -> 
            "Error de conexión. Verifica tu acceso a internet."
        message.contains("unauthorized") || message.contains("jwt") || message.contains("session") -> 
            "Sesión expirada o no autorizada. Por favor, inicia sesión nuevamente."
        else -> this?.message?.takeIf { it.isNotBlank() && !it.contains("exception", ignoreCase = true) } ?: defaultMessage
    }
}
