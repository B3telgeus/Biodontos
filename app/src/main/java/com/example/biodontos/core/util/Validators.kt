package com.example.biodontos.core.util

object Validators {
    private val NAME_REGEX = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$")
    private val DIGITS_ONLY_REGEX = Regex("^[0-9]+$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val LICENSE_REGEX = Regex("^[a-zA-Z0-9-]+$")

    fun validateName(value: String, fieldName: String = "nombre"): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "El $fieldName es obligatorio"
        if (trimmed.length < 2) return "El $fieldName debe tener al menos 2 caracteres"
        if (!NAME_REGEX.matches(trimmed)) return "Solo se permiten letras y espacios (carácter inválido)"
        return null
    }

    fun validateDni(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "El DNI / Cédula es obligatorio"
        if (!DIGITS_ONLY_REGEX.matches(trimmed)) return "Solo se permiten números (carácter inválido)"
        if (trimmed.length < 5 || trimmed.length > 15) return "El documento debe tener entre 5 y 15 dígitos"
        return null
    }

    fun validateEmail(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "El correo electrónico es obligatorio"
        if (trimmed.contains(" ")) return "El correo no puede contener espacios"
        if (!EMAIL_REGEX.matches(trimmed)) return "Formato no válido (ejemplo: usuario@correo.com)"
        return null
    }

    fun validatePhone(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null // Opcional
        if (!DIGITS_ONLY_REGEX.matches(trimmed)) return "Solo se permiten números (carácter inválido)"
        if (trimmed.length < 7 || trimmed.length > 15) return "El teléfono debe tener entre 7 y 15 dígitos"
        return null
    }

    fun validateLicense(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "La cédula / tarjeta profesional es obligatoria"
        if (!LICENSE_REGEX.matches(trimmed)) return "Solo se permiten caracteres alfanuméricos y guiones"
        if (trimmed.length < 3) return "Debe tener al menos 3 caracteres"
        return null
    }

    fun validatePassword(value: String): String? {
        if (value.isEmpty()) return "La contraseña no puede estar vacía"
        if (value.length < 6) return "La contraseña debe tener mínimo 6 caracteres"
        return null
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        if (confirmPassword.isEmpty()) return "Confirma tu contraseña"
        if (password != confirmPassword) return "Las contraseñas no coinciden"
        return null
    }

    fun isValidName(value: String): Boolean = validateName(value) == null
    fun isValidDni(value: String): Boolean = validateDni(value) == null
    fun isValidEmail(value: String): Boolean = validateEmail(value) == null
    fun isValidPhone(value: String): Boolean = validatePhone(value) == null
    fun isValidLicenseNumber(value: String): Boolean = validateLicense(value) == null
    fun isValidPassword(value: String): Boolean = validatePassword(value) == null
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean = validateConfirmPassword(password, confirmPassword) == null
}
