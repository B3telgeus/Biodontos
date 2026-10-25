package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.ClinicalAuditLogDto
import com.example.biodontos.data.dto.EvolutionDto
import com.example.biodontos.data.dto.PaymentDto
import com.example.biodontos.data.dto.toDomain
import com.example.biodontos.domain.model.history.AuditAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalAuditLogMapperTest {

    @Test
    fun `ClinicalAuditLogDto maps accurately to AuditEntry domain model`() {
        val dto = ClinicalAuditLogDto(
            id = "aud-100",
            patientId = "pat-1",
            entityTable = "evolutions",
            entityId = "evo-55",
            action = "VOID",
            changedFields = listOf("voided_at", "void_reason"),
            actorUserId = "usr-9",
            actorName = "Dra. Laura Ríos",
            occurredAt = "2026-10-25T14:20:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("aud-100", domain.id)
        assertEquals("pat-1", domain.patientId)
        assertEquals("evolutions", domain.entityTable)
        assertEquals("evo-55", domain.entityId)
        assertEquals(AuditAction.VOID, domain.action)
        assertEquals("Dra. Laura Ríos", domain.actorName)
        assertEquals(2, domain.changedFields.size)
    }

    @Test
    fun `EvolutionDto maps voided fields and clarification link to domain`() {
        val dto = EvolutionDto(
            id = "evo-2",
            recordId = "rec-1",
            doctorId = "doc-1",
            date = "2026-10-25",
            procedureTime = "09:15:00",
            description = "NOTA ACLARATORIA: Se corrige numeración FDI",
            voidedAt = "2026-10-25T15:00:00Z",
            voidedBy = "doc-1",
            voidReason = "Registro duplicado por falla de red",
            amendsEvolutionId = "evo-1"
        )

        val domain = dto.toDomain()

        assertTrue(domain.isVoided)
        assertTrue(domain.isClarification)
        assertEquals("evo-1", domain.amendsEvolutionId)
        assertNotNull(domain.voidInfo)
        assertEquals("Registro duplicado por falla de red", domain.voidInfo?.reason)
    }

    @Test
    fun `PaymentDto maps voidInfo when voidedAt is present and null otherwise`() {
        val activeDto = PaymentDto(
            id = "pay-1",
            treatmentId = "tr-1",
            amount = 100000.0
        )
        assertNull(activeDto.toDomain().voidInfo)

        val voidedDto = PaymentDto(
            id = "pay-2",
            treatmentId = "tr-1",
            amount = 100000.0,
            voidedAt = "2026-10-25T16:00:00Z",
            voidReason = "Error en monto digitado"
        )
        val domain = voidedDto.toDomain()
        assertTrue(domain.isVoided)
        assertEquals("Error en monto digitado", domain.voidInfo?.reason)
    }
}
