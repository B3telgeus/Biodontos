package com.example.biodontos.core.util

object Validators {
    private val NAME_REGEX = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$")
    private val DIGITS_ONLY_REGEX = Regex("^[0-9]+$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val LICENSE_REGEX = Regex("^[a-zA-Z0-9-]+$")

    fun validateName(value: String, isLastName: Boolean = false): String? {
        val trimmed = value.trim()
        val label = if (isLastName) "apellido" else "nombre"
        if (trimmed.isEmpty()) return "El $label es obligatorio"
        if (trimmed.length < 2) return "Error en el $label"
        if (!NAME_REGEX.matches(trimmed)) return "Error en el $label"
        return null
    }

    fun validateDni(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "El DNI / Cédula es obligatorio"
        if (!DIGITS_ONLY_REGEX.matches(trimmed)) return "Error en el DNI"
        if (trimmed.length < 5 || trimmed.length > 15) return "Error en el DNI"
        return null
    }

    fun validateEmail(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "El correo electrónico es obligatorio"
        if (trimmed.contains(" ")) return "Error en el correo"
        if (!EMAIL_REGEX.matches(trimmed)) return "Error en el correo"
        return null
    }

    fun validatePhone(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null // Opcional
        if (!DIGITS_ONLY_REGEX.matches(trimmed)) return "Error en el teléfono"
        if (trimmed.length < 7 || trimmed.length > 15) return "Error en el teléfono"
        return null
    }

    fun validateLicense(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "La cédula / tarjeta profesional es obligatoria"
        if (!LICENSE_REGEX.matches(trimmed)) return "Error en la cédula profesional"
        if (trimmed.length < 3) return "Error en la cédula profesional"
        return null
    }

    fun validatePassword(value: String): String? {
        if (value.isEmpty()) return "La contraseña no puede estar vacía"
        if (value.length < 6) return "Error en la contraseña (mínimo 6 caracteres)"
        return null
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        if (confirmPassword.isEmpty()) return "Confirma tu contraseña"
        if (password != confirmPassword) return "Las contraseñas no coinciden"
        return null
    }

    fun isValidName(value: String, isLastName: Boolean = false): Boolean = validateName(value, isLastName) == null
    fun isValidDni(value: String): Boolean = validateDni(value) == null
    fun isValidEmail(value: String): Boolean = validateEmail(value) == null
    fun isValidPhone(value: String): Boolean = validatePhone(value) == null
    fun isValidLicenseNumber(value: String): Boolean = validateLicense(value) == null
    fun isValidPassword(value: String): Boolean = validatePassword(value) == null
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean = validateConfirmPassword(password, confirmPassword) == null
}
