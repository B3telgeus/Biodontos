package com.example.biodontos.domain.usecase.clinical

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckConsentLockUseCaseTest {

    private val useCase = CheckConsentLockUseCase()

    @Test
    fun `invoke returns success when consent is signed`() {
        val result = useCase(isConsentSigned = true)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke returns failure when consent is not signed`() {
        val result = useCase(isConsentSigned = false)

        assertTrue(result.isFailure)
        assertEquals(
            "Acción Bloqueada: Falta Consentimiento Informado.",
            result.exceptionOrNull()?.message
        )
    }
}
