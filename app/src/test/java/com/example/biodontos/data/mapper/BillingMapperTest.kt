package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.PaymentDto
import com.example.biodontos.data.dto.TreatmentDto
import com.example.biodontos.data.dto.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BillingMapperTest {

    @Test
    fun `TreatmentDto toDomain maps fields accurately`() {
        val dto = TreatmentDto(
            id = "tr-101",
            patientId = "pat-202",
            description = "Endodoncia unirradicular",
            totalCost = 350000.0,
            status = "PENDIENTE",
            createdAt = "2026-10-20T14:30:00"
        )

        val domain = dto.toDomain()

        assertEquals("tr-101", domain.id)
        assertEquals("pat-202", domain.patientId.value)
        assertEquals("Endodoncia unirradicular", domain.description)
        assertEquals(350000L, domain.totalCost.pesos)
        assertEquals("PENDIENTE", domain.status)
        assertNotNull(domain.createdAt)
    }

    @Test
    fun `PaymentDto toDomain maps fields and default paymentMethod accurately`() {
        val dto = PaymentDto(
            id = "pay-501",
            treatmentId = "tr-101",
            amount = 150000.0,
            paymentMethod = null,
            createdAt = "2026-10-21T09:15:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("pay-501", domain.id)
        assertEquals("tr-101", domain.treatmentId)
        assertEquals(150000L, domain.amount.pesos)
        assertEquals("EFECTIVO", domain.paymentMethod)
        assertNotNull(domain.createdAt)
    }
}
