package com.example.biodontos.domain.model.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreatmentStatusTest {

    @Test
    fun `state machine allows valid forward transitions and cancellation`() {
        assertTrue(TreatmentStatus.PENDIENTE.canTransitionTo(TreatmentStatus.APROBADO))
        assertTrue(TreatmentStatus.PENDIENTE.canTransitionTo(TreatmentStatus.CANCELADO))
        assertFalse(TreatmentStatus.PENDIENTE.canTransitionTo(TreatmentStatus.COMPLETADO))

        assertTrue(TreatmentStatus.APROBADO.canTransitionTo(TreatmentStatus.EN_CURSO))
        assertTrue(TreatmentStatus.APROBADO.canTransitionTo(TreatmentStatus.CANCELADO))
        assertFalse(TreatmentStatus.APROBADO.canTransitionTo(TreatmentStatus.PENDIENTE))

        assertTrue(TreatmentStatus.EN_CURSO.canTransitionTo(TreatmentStatus.COMPLETADO))
        assertTrue(TreatmentStatus.EN_CURSO.canTransitionTo(TreatmentStatus.CANCELADO))
    }

    @Test
    fun `terminal states COMPLETADO and CANCELADO disallow any further transitions`() {
        assertTrue(TreatmentStatus.COMPLETADO.availableTransitions().isEmpty())
        assertTrue(TreatmentStatus.CANCELADO.availableTransitions().isEmpty())
    }

    @Test
    fun `fromString normalizes unknown or blank values to PENDIENTE`() {
        assertEquals(TreatmentStatus.APROBADO, TreatmentStatus.fromString("aprobado"))
        assertEquals(TreatmentStatus.PENDIENTE, TreatmentStatus.fromString("UNKNOWN"))
    }
}
