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
        message.contains("idx_appointments_doctor_slot_unique") || message.contains("duplicate key") ->
            "El horario seleccionado ya fue reservado por otro paciente. Por favor selecciona otra franja horaria."
        message.contains("trg_validate_appointment_update") || message.contains("sólo el doctor asignado") || message.contains("solo el doctor asignado") ->
            "Operación no autorizada para el estado actual de la cita."
        message.contains("network") || message.contains("connect") || message.contains("timeout") -> 
            "Error de conexión. Verifica tu acceso a internet."
        message.contains("could not find the table") || message.contains("doctor_availability") || message.contains("pgrst200") ->
            "La tabla de horarios está en proceso de sincronización con la nube. Por favor, ejecuta el script de migración SQL en Supabase."
        message.contains("invalid input syntax for uuid") ->
            "Identificador de doctor o paciente con formato inválido. Verifique la sesión activa."
        message.contains("informed_consents_doctor_id_fkey") ->
            "El odontólogo asignado no se encuentra registrado en el sistema clínico."
        message.contains("periodontal_examinations_doctor_id_fkey") || message.contains("preiodontal_examinations_doctor_id_fkey") ->
            "El odontólogo responsable no se encuentra registrado en el sistema clínico. Verifique la sesión o ejecute la migración de especialistas."
        message.contains("unauthorized") || message.contains("jwt") || message.contains("session") -> 
            "Sesión expirada o no autorizada. Por favor, inicia sesión nuevamente."
        else -> this?.message?.takeIf { it.isNotBlank() && !it.contains("exception", ignoreCase = true) } ?: defaultMessage
    }
}

