package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.PaymentDto
import com.example.biodontos.data.dto.TreatmentDto
import com.example.biodontos.data.dto.toDomain
import com.example.biodontos.data.dto.toDto
import com.example.biodontos.domain.model.CopAmount
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.Payment
import com.example.biodontos.domain.model.Treatment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Suite de pruebas unitarias que valida que los payloads de Treatments y Payments enviados
 * a PostgreSQL / Supabase cumplan con los headers de base de datos exactos,
 * compatibilidad de ENUM treatment_status y tipos UUID seguros.
 */
class BillingDatabasePayloadValidationTest {

    private val supabaseSerializerJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val strictNullsJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
    }

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    @Test
    fun treatmentPayloadContainsAllDatabaseHeadersAndNoCamelCaseLeaks() {
        val patientUuid = "7c9e6679-7425-40de-944b-e07fc1f90ae7"
        val treatmentUuid = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"

        val treatment = Treatment(
            id = treatmentUuid,
            patientId = PatientId(patientUuid),
            description = "Ortodoncia con Brackets Autoligados",
            totalCost = CopAmount(2_800_000L),
            status = "PENDIENTE",
            createdAt = LocalDateTime.of(2026, 10, 20, 10, 0)
        )

        val dto = treatment.toDto()
        val jsonString = strictNullsJson.encodeToString(dto)
        val jsonObject = strictNullsJson.parseToJsonElement(jsonString).jsonObject
        val headers = jsonObject.keys

        // 1. Columnas obligatorias de la tabla treatments en Postgres
        val expectedHeaders = listOf("id", "patient_id", "description", "total_cost", "status", "created_at")
        for (header in expectedHeaders) {
            assertTrue("El payload debe incluir el header '$header'", headers.contains(header))
        }

        // 2. Ausencia de fugas camelCase
        assertFalse("NO debe incluir camelCase patientId", headers.contains("patientId"))
        assertFalse("NO debe incluir camelCase totalCost", headers.contains("totalCost"))
        assertFalse("NO debe incluir camelCase createdAt", headers.contains("createdAt"))

        // 3. Verificación de valores y tipos
        assertEquals(patientUuid, jsonObject["patient_id"]?.jsonPrimitive?.content)
        assertEquals("Ortodoncia con Brackets Autoligados", jsonObject["description"]?.jsonPrimitive?.content)
        assertEquals(2800000.0, jsonObject["total_cost"]?.jsonPrimitive?.doubleOrNull ?: 0.0, 0.001)
        assertEquals("PENDIENTE", jsonObject["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun treatmentStatusConformsToPostgresTreatmentStatusEnum() {
        // En PostgreSQL el tipo ENUM treatment_status solo admite:
        // 'PENDIENTE', 'EN_PROGRESO', 'COMPLETADO', 'CANCELADO'
        // Si se enviara 'ACTIVE' fallaría en base de datos.
        val validPostgresStatuses = listOf("PENDIENTE", "EN_PROGRESO", "COMPLETADO", "CANCELADO")

        val treatmentWithActive = Treatment(
            patientId = PatientId("7c9e6679-7425-40de-944b-e07fc1f90ae7"),
            description = "Blanqueamiento dental",
            totalCost = CopAmount(500_000L),
            status = "ACTIVE" // Valor antiguo no compatible con enum
        )

        val dto = treatmentWithActive.toDto()
        // El mapper debe normalizarlo al enum de Postgres
        assertTrue(
            "El estado '${dto.status}' debe ser uno de los válidos en PostgreSQL: $validPostgresStatuses",
            validPostgresStatuses.contains(dto.status)
        )
        assertEquals("PENDIENTE", dto.status)
    }

    @Test
    fun paymentPayloadContainsAllDatabaseHeadersAndNoCamelCaseLeaks() {
        val treatmentUuid = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"
        val paymentUuid = "f47ac10b-58cc-4372-a567-0e02b2c3d479"

        val payment = Payment(
            id = paymentUuid,
            treatmentId = treatmentUuid,
            amount = CopAmount(350_000L),
            paymentMethod = "EFECTIVO",
            createdAt = LocalDateTime.of(2026, 10, 20, 11, 30)
        )

        val dto = payment.toDto()
        val jsonString = strictNullsJson.encodeToString(dto)
        val jsonObject = strictNullsJson.parseToJsonElement(jsonString).jsonObject
        val headers = jsonObject.keys

        // 1. Columnas obligatorias de la tabla payments en Postgres
        val expectedHeaders = listOf("id", "treatment_id", "amount", "payment_method", "created_at")
        for (header in expectedHeaders) {
            assertTrue("El payload debe incluir el header '$header'", headers.contains(header))
        }

        // 2. Ausencia de fugas camelCase
        assertFalse("NO debe incluir camelCase treatmentId", headers.contains("treatmentId"))
        assertFalse("NO debe incluir camelCase paymentMethod", headers.contains("paymentMethod"))
        assertFalse("NO debe incluir camelCase createdAt", headers.contains("createdAt"))

        // 3. Verificación de valores
        assertEquals(treatmentUuid, jsonObject["treatment_id"]?.jsonPrimitive?.content)
        assertEquals(350000.0, jsonObject["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0, 0.001)
        assertEquals("EFECTIVO", jsonObject["payment_method"]?.jsonPrimitive?.content)
    }

    @Test
    fun emptyIdSanitizedToNullEnablingPostgresUuidDefaultGeneration() {
        val newTreatment = Treatment(
            id = "   ",
            patientId = PatientId("7c9e6679-7425-40de-944b-e07fc1f90ae7"),
            description = "Nuevo tratamiento",
            totalCost = CopAmount(100_000L)
        )
        val trDto = newTreatment.toDto()
        assertNull("id vacío debe ser null en DTO para que Postgres use uuid_generate_v4()", trDto.id)

        val newPayment = Payment(
            id = "",
            treatmentId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
            amount = CopAmount(50_000L)
        )
        val payDto = newPayment.toDto()
        assertNull("id vacío en Payment debe ser null", payDto.id)

        val strictJson = strictNullsJson.encodeToString(payDto)
        val jsonObj = strictNullsJson.parseToJsonElement(strictJson).jsonObject
        assertTrue("id debe serializar como JsonNull", jsonObj["id"] is JsonNull)
    }

    @Test
    fun billingRoundTripSerializationPreservesValuesAccurately() {
        val originalPayment = Payment(
            id = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            treatmentId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
            amount = CopAmount(750_000L),
            paymentMethod = "TRANSFERENCIA",
            createdAt = LocalDateTime.of(2026, 10, 21, 14, 0)
        )

        val serialized = supabaseSerializerJson.encodeToString(originalPayment.toDto())
        val deserializedDto = supabaseSerializerJson.decodeFromString<PaymentDto>(serialized)
        val domainReconstructed = deserializedDto.toDomain()

        assertEquals(originalPayment.id, domainReconstructed.id)
        assertEquals(originalPayment.treatmentId, domainReconstructed.treatmentId)
        assertEquals(originalPayment.amount.pesos, domainReconstructed.amount.pesos)
        assertEquals(originalPayment.paymentMethod, domainReconstructed.paymentMethod)
        assertEquals(originalPayment.createdAt, domainReconstructed.createdAt)
    }
}
