package com.example.biodontos.core.util

import androidx.annotation.StringRes
import com.example.biodontos.R
import com.example.biodontos.core.ui.UiText
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

sealed interface AppError {
    @get:StringRes
    val messageRes: Int
    val defaultText: String

    fun asUiText(): UiText = UiText.StringResource(messageRes)

    data object UserAlreadyRegistered : AppError {
        override val messageRes: Int = R.string.error_user_already_registered
        override val defaultText: String = "Este correo electrónico ya está registrado en el sistema."
    }

    data object InvalidCredentials : AppError {
        override val messageRes: Int = R.string.error_invalid_credentials
        override val defaultText: String = "Correo o contraseña incorrectos. Verifica tus datos."
    }

    data object WeakPassword : AppError {
        override val messageRes: Int = R.string.error_weak_password
        override val defaultText: String = "La contraseña es demasiado débil o no cumple con los requisitos mínimos."
    }

    data object InvalidNameFormat : AppError {
        override val messageRes: Int = R.string.error_invalid_name_format
        override val defaultText: String = "Error en el nombre o formato de texto ingresado."
    }

    data object DuplicateMaterial : AppError {
        override val messageRes: Int = R.string.error_duplicate_material
        override val defaultText: String = "Ya existe un material activo registrado con el mismo número de lote y registro INVIMA."
    }

    data object InvalidMaterialExpirationDate : AppError {
        override val messageRes: Int = R.string.error_invalid_material_expiration
        override val defaultText: String = "La fecha de vencimiento debe ser posterior a la fecha de elaboración."
    }

    data object AppointmentSlotTaken : AppError {
        override val messageRes: Int = R.string.error_appointment_slot_taken
        override val defaultText: String = "El horario seleccionado ya fue reservado por otro paciente. Por favor selecciona otra franja horaria."
    }

    data object UnauthorizedAppointmentOperation : AppError {
        override val messageRes: Int = R.string.error_unauthorized_appointment_op
        override val defaultText: String = "Operación no autorizada para el estado actual de la cita."
    }

    data object NetworkConnection : AppError {
        override val messageRes: Int = R.string.error_network_connection
        override val defaultText: String = "Error de conexión. Verifica tu acceso a internet."
    }

    data object Timeout : AppError {
        override val messageRes: Int = R.string.error_network_timeout
        override val defaultText: String = "La solicitud tardó demasiado tiempo. Verifica tu conexión e intenta nuevamente."
    }

    data object InvalidUuidFormat : AppError {
        override val messageRes: Int = R.string.error_invalid_uuid_format
        override val defaultText: String = "Identificador de doctor o paciente con formato inválido. Verifique la sesión activa."
    }

    data object DoctorNotRegistered : AppError {
        override val messageRes: Int = R.string.error_doctor_not_registered
        override val defaultText: String = "El odontólogo asignado no se encuentra registrado en el sistema clínico."
    }

    data object UnauthorizedSession : AppError {
        override val messageRes: Int = R.string.error_unauthorized_session
        override val defaultText: String = "Sesión expirada o no autorizada. Por favor, inicia sesión nuevamente."
    }

    data object ServiceUnavailable : AppError {
        override val messageRes: Int = R.string.error_service_unavailable
        override val defaultText: String = "El servicio no se encuentra disponible temporalmente. Por favor, intenta más tarde."
    }

    data object Unknown : AppError {
        override val messageRes: Int = R.string.error_unexpected_default
        override val defaultText: String = "Ocurrió un error inesperado. Por favor, intenta de nuevo."
    }
}

fun Throwable?.toAppError(): AppError {
    if (this == null) return AppError.Unknown

    return when (this) {
        is RestException -> mapRestException(this)
        is HttpRequestTimeoutException,
        is SocketTimeoutException,
        is TimeoutException -> AppError.Timeout
        is HttpRequestException,
        is IOException -> AppError.NetworkConnection
        else -> mapFallbackMessage(this.message)
    }
}

@Suppress("CyclomaticComplexMethod")
private fun mapRestException(ex: RestException): AppError {
    val errorCode = ex.error.trim().uppercase()
    val context = listOfNotNull(ex.error, ex.description, ex.message).joinToString(" ").lowercase()

    when (errorCode) {
        "23505" -> {
            return if (context.contains("ux_clinical_materials") || context.contains("invima") || context.contains("batch_lot")) {
                AppError.DuplicateMaterial
            } else {
                AppError.AppointmentSlotTaken
            }
        }
        "23514" -> {
            return if (context.contains("chk_clinical_materials") || context.contains("expiration") || context.contains("elaboraci")) {
                AppError.InvalidMaterialExpirationDate
            } else {
                AppError.Unknown
            }
        }
        "23503" -> {
            return if (context.contains("doctor_id_fkey") || context.contains("doctors")) {
                AppError.DoctorNotRegistered
            } else {
                AppError.Unknown
            }
        }
        "22P02" -> return AppError.InvalidUuidFormat
        "42501" -> {
            return if (context.contains("trg_validate_appointment_update") || context.contains("appointment")) {
                AppError.UnauthorizedAppointmentOperation
            } else {
                AppError.UnauthorizedSession
            }
        }
        "PGRST301", "PGRST302" -> return AppError.UnauthorizedSession
        "PGRST200", "PGRST204", "42P01", "42703" -> return AppError.ServiceUnavailable
        "INVALID_GRANT" -> return AppError.InvalidCredentials
    }

    return when (ex) {
        is UnauthorizedRestException -> {
            if (context.contains("invalid login credentials") ||
                context.contains("invalid credentials") ||
                context.contains("invalid_grant") ||
                context.contains("invalid grant")
            ) {
                AppError.InvalidCredentials
            } else {
                AppError.UnauthorizedSession
            }
        }
        is NotFoundRestException -> AppError.ServiceUnavailable
        is BadRequestRestException -> mapFallbackMessage(context)
        else -> mapFallbackMessage(context)
    }
}

@Suppress("CyclomaticComplexMethod")
private fun mapFallbackMessage(rawMessage: String?): AppError {
    val message = rawMessage?.lowercase() ?: return AppError.Unknown
    return when {
        message.contains("user already registered") || message.contains("already registered") ->
            AppError.UserAlreadyRegistered
        message.contains("invalid login credentials") ||
            message.contains("invalid credentials") ||
            message.contains("invalid grant") ||
            message.contains("invalid_grant") ||
            message.contains("credenciales inválidas") ||
            message.contains("credenciales invalidas") ->
            AppError.InvalidCredentials
        message.contains("password should be at least") || message.contains("weak password") ->
            AppError.WeakPassword
        message.contains("nameerror") || message.contains("name error") ->
            AppError.InvalidNameFormat
        message.contains("ux_clinical_materials") ->
            AppError.DuplicateMaterial
        message.contains("chk_clinical_materials") || message.contains("posterior a la fecha de elaboración") ->
            AppError.InvalidMaterialExpirationDate
        message.contains("idx_appointments_doctor_slot_unique") || message.contains("duplicate key") ->
            AppError.AppointmentSlotTaken
        message.contains("trg_validate_appointment_update") ||
            message.contains("sólo el doctor asignado") ||
            message.contains("solo el doctor asignado") ->
            AppError.UnauthorizedAppointmentOperation
        message.contains("timeout") || message.contains("timed out") ->
            AppError.Timeout
        message.contains("network") || message.contains("connect") || message.contains("unreachable") ->
            AppError.NetworkConnection
        message.contains("could not find the table") ||
            message.contains("schema cache") ||
            message.contains("pgrst200") ||
            message.contains("pgrst204") ->
            AppError.ServiceUnavailable
        message.contains("invalid input syntax for uuid") || message.contains("invalid input syntax for type uuid") ->
            AppError.InvalidUuidFormat
        message.contains("informed_consents_doctor_id_fkey") ||
            message.contains("periodontal_examinations_doctor_id_fkey") ||
            message.contains("preiodontal_examinations_doctor_id_fkey") ->
            AppError.DoctorNotRegistered
        message.contains("unauthorized") || message.contains("jwt") || message.contains("session") ->
            AppError.UnauthorizedSession
        else -> AppError.Unknown
    }
}

fun Throwable?.toUserFriendlyMessage(
    defaultMessage: String = AppError.Unknown.defaultText
): String {
    return when (val appError = this.toAppError()) {
        AppError.Unknown -> defaultMessage
        else -> appError.defaultText
    }
}
