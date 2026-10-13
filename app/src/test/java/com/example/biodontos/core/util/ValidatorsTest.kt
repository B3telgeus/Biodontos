package com.example.biodontos.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun validateName_withValidCharacters_returnsNull() {
        assertNull(Validators.validateName("Carlos Andrés", false))
        assertNull(Validators.validateName("María José Peñaloza", false))
        assertNull(Validators.validateName("O'Connor", true))
    }

    @Test
    fun validateName_withInvalidCharacters_returnsError() {
        assertNotNull(Validators.validateName("Carlos123", false))
        assertNotNull(Validators.validateName("Ana@Lopez", false))
        assertEquals("Error en el nombre", Validators.validateName("Carlos123", false))
    }

    @Test
    fun validateDni_withValidDigits_returnsNull() {
        assertNull(Validators.validateDni("1020304050"))
    }

    @Test
    fun validateDni_withLettersOrSymbols_returnsError() {
        val error = Validators.validateDni("102030A50")
        assertNotNull(error)
        assertEquals("Error en el DNI", error)
    }

    @Test
    fun validateEmail_withValidFormat_returnsNull() {
        assertNull(Validators.validateEmail("doctor@biodontos.com"))
        assertNull(Validators.validateEmail("paciente.123@gmail.com"))
    }

    @Test
    fun validateEmail_withSpacesOrInvalidFormat_returnsError() {
        assertNotNull(Validators.validateEmail("doctor @biodontos.com"))
        assertNotNull(Validators.validateEmail("correo-sin-arroba.com"))
    }

    @Test
    fun validatePhone_withLetters_returnsError() {
        assertNotNull(Validators.validatePhone("300123abc4"))
    }

    @Test
    fun validatePassword_withLessThan6Chars_returnsError() {
        val error = Validators.validatePassword("12345")
        assertNotNull(error)
        assertEquals("Error en la contraseña (mínimo 6 caracteres)", error)
    }

    @Test
    fun validateConfirmPassword_mismatch_returnsError() {
        val error = Validators.validateConfirmPassword("Password123", "Password456")
        assertNotNull(error)
        assertEquals("Las contraseñas no coinciden", error)
    }

    @Test
    fun validateConfirmPassword_match_returnsNull() {
        assertNull(Validators.validateConfirmPassword("Password123", "Password123"))
    }
}
