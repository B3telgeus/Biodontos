package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.AppointmentDto
import com.example.biodontos.data.dto.DoctorAvailabilityDto
import com.example.biodontos.data.dto.DoctorDto
import com.example.biodontos.data.dto.EvolutionDto
import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.Patient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
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
 * Suite de pruebas automatizadas que valida de forma rigurosa que los datos enviados
 * a la base de datos (PostgreSQL / Supabase) cumplan con todos los headers de columnas,
 * nomenclatura exacta (@SerialName snake_case) y sanitización estricta de tipos UUID
 * para patient_id, doctor_id y claves foráneas asociadas.
 *
 * Previene:
 * 1. Error Postgres 42703 (columna inexistente por fugas camelCase como patientId o doctorId).
 * 2. Error Postgres 22P02 (invalid input syntax for type uuid: "") cuando Kotlin envía strings vacíos.
 * 3. Violaciones de restricción NOT NULL en patient_id.
 */
class AppointmentDatabasePayloadValidationTest {

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
    fun appointmentsPayloadContainsAllRequiredDatabaseHeadersAndNoCamelCaseLeaks() {
        val validPatientUuid = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22"
        val validDoctorUuid = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33"
        val validEvolutionUuid = "d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44"
        val validAppointmentUuid = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"

        val appointment = Appointment(
            id = validAppointmentUuid,
            patientId = validPatientUuid,
            doctorId = validDoctorUuid,
            originEvolutionId = validEvolutionUuid,
            startTime = LocalDateTime.of(2026, 10, 15, 10, 0),
            endTime = LocalDateTime.of(2026, 10, 15, 10, 30),
            status = AppointmentStatus.PENDING,
            isConfirmed = false,
            reason = "Revisión general y profilaxis"
        )

        val dto = appointment.toDto()
        val jsonString = strictNullsJson.encodeToString(dto)
        val jsonObject = strictNullsJson.parseToJsonElement(jsonString).jsonObject
        val payloadHeaders = jsonObject.keys

        // 1. Verificación de Headers exactos según DDL de la tabla appointments en PostgreSQL
        val expectedHeaders = listOf(
            "id",
            "patient_id",
            "doctor_id",
            "origin_evolution_id",
            "start_time",
            "end_time",
            "status",
            "confirmation_status",
            "reason"
        )
        for (header in expectedHeaders) {
            assertTrue("El payload debe incluir el header de base de datos '$header'", payloadHeaders.contains(header))
        }

        // 2. Verificación estricta de que NO existan fugas camelCase de Kotlin
        val forbiddenCamelCaseProperties = listOf(
            "patientId",
            "doctorId",
            "originEvolutionId",
            "startTime",
            "endTime",
            "isConfirmed",
            "confirmationStatus"
        )
        for (leak in forbiddenCamelCaseProperties) {
            assertFalse("El payload serializado NO debe contener la propiedad camelCase '$leak'", payloadHeaders.contains(leak))
        }

        // 3. Verificación de valores y formatos de IDs
        assertEquals(validPatientUuid, jsonObject["patient_id"]?.jsonPrimitive?.content)
        assertEquals(validDoctorUuid, jsonObject["doctor_id"]?.jsonPrimitive?.content)
        assertEquals(validEvolutionUuid, jsonObject["origin_evolution_id"]?.jsonPrimitive?.content)
        assertEquals("2026-10-15T10:00:00", jsonObject["start_time"]?.jsonPrimitive?.content)
        assertEquals("2026-10-15T10:30:00", jsonObject["end_time"]?.jsonPrimitive?.content)
        assertEquals("SCHEDULED", jsonObject["status"]?.jsonPrimitive?.content)
        assertEquals(false, jsonObject["confirmation_status"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("Revisión general y profilaxis", jsonObject["reason"]?.jsonPrimitive?.content)
    }

    @Test
    fun doctorIdEmptyOrBlankStringIsSanitizedToNullPreventingPostgresUuidError() {
        // En PostgreSQL un doctor_id con string vacío ("") genera error 22P02:
        // invalid input syntax for type uuid: ""
        val testCasesWithBlankDoctor = listOf("", "   ", null)

        for (blankDoctorId in testCasesWithBlankDoctor) {
            val appointment = Appointment(
                patientId = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
                doctorId = blankDoctorId,
                startTime = LocalDateTime.of(2026, 10, 16, 9, 0)
            )

            val dto = appointment.toDto()

            // En el DTO debe haberse normalizado a null
            assertNull("Un doctorId vacío o con espacios debe mapearse a null en el DTO", dto.doctorId)

            // Con el serializador real de Supabase (explicitNulls = false), no debe enviar la clave
            val supabaseJsonString = supabaseSerializerJson.encodeToString(dto)
            val supabaseJsonObject = supabaseSerializerJson.parseToJsonElement(supabaseJsonString).jsonObject
            assertFalse("Con explicitNulls=false, doctor_id nulo no debe estar presente como clave", supabaseJsonObject.containsKey("doctor_id"))

            // Con serializador estricto (explicitNulls = true), debe ser JsonNull y NUNCA un string vacío ""
            val strictJsonString = strictNullsJson.encodeToString(dto)
            val strictJsonObject = strictNullsJson.parseToJsonElement(strictJsonString).jsonObject
            val doctorIdElement = strictJsonObject["doctor_id"]
            assertTrue("doctor_id debe ser JsonNull cuando no hay doctor asignado", doctorIdElement is JsonNull)
            assertFalse("doctor_id NUNCA debe ser un string vacío \"\"", doctorIdElement?.jsonPrimitive?.content == "")
        }
    }

    @Test
    fun originEvolutionIdAndAppointmentIdSanitizedToNullWhenBlank() {
        val appointment = Appointment(
            id = "   ",
            patientId = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
            doctorId = null,
            originEvolutionId = "",
            startTime = LocalDateTime.of(2026, 10, 17, 14, 0)
        )

        val dto = appointment.toDto()

        assertNull("id con espacios en blanco debe mapearse a null para permitir gen_random_uuid() en DB", dto.id)
        assertNull("originEvolutionId vacío debe mapearse a null para no romper sintaxis uuid", dto.originEvolutionId)

        val jsonString = strictNullsJson.encodeToString(dto)
        val jsonObject = strictNullsJson.parseToJsonElement(jsonString).jsonObject

        assertTrue("id debe ser JsonNull", jsonObject["id"] is JsonNull)
        assertTrue("origin_evolution_id debe ser JsonNull", jsonObject["origin_evolution_id"] is JsonNull)
    }

    @Test
    fun patientIdAndDoctorIdComplyWithRfc4122UuidStandard() {
        val patientUuid = "7c9e6679-7425-40de-944b-e07fc1f90ae7"
        val doctorUuid = "6ba7b810-9dad-11d1-80b4-00c04fd430c8"

        val appointment = Appointment(
            patientId = "  $patientUuid  ", // con espacios accidentales
            doctorId = "  $doctorUuid  ",
            startTime = LocalDateTime.of(2026, 10, 18, 11, 0)
        )

        val dto = appointment.toDto()

        // Deben ser recortados limpiamente
        assertEquals(patientUuid, dto.patientId)
        assertEquals(doctorUuid, dto.doctorId)

        // Deben cumplir con el estándar UUID
        assertTrue("patientId debe ser un UUID válido", uuidRegex.matches(dto.patientId))
        assertTrue("doctorId debe ser un UUID válido", dto.doctorId?.let { uuidRegex.matches(it) } == true)
    }

    @Test
    fun appointmentConfirmationStatusHeaderAndStateMapping() {
        // Caso 1: Cita PENDING sin confirmación
        val pendingAppt = Appointment(
            patientId = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
            startTime = LocalDateTime.of(2026, 10, 19, 8, 0),
            status = AppointmentStatus.PENDING,
            isConfirmed = false
        )
        val pendingJson = strictNullsJson.parseToJsonElement(strictNullsJson.encodeToString(pendingAppt.toDto())).jsonObject
        assertEquals("SCHEDULED", pendingJson["status"]?.jsonPrimitive?.content)
        assertEquals(false, pendingJson["confirmation_status"]?.jsonPrimitive?.booleanOrNull)

        // Caso 2: Cita con confirmación activada
        val confirmedAppt = Appointment(
            patientId = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
            startTime = LocalDateTime.of(2026, 10, 19, 8, 0),
            status = AppointmentStatus.CONFIRMED,
            isConfirmed = true
        )
        val confirmedJson = strictNullsJson.parseToJsonElement(strictNullsJson.encodeToString(confirmedAppt.toDto())).jsonObject
        assertEquals(true, confirmedJson["confirmation_status"]?.jsonPrimitive?.booleanOrNull)

        // Caso 3: Cita cancelada
        val cancelledAppt = Appointment(
            patientId = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
            startTime = LocalDateTime.of(2026, 10, 19, 8, 0),
            status = AppointmentStatus.CANCELLED
        )
        val cancelledJson = strictNullsJson.parseToJsonElement(strictNullsJson.encodeToString(cancelledAppt.toDto())).jsonObject
        assertEquals("CANCELLED", cancelledJson["status"]?.jsonPrimitive?.content)

        // Caso 4: Cita completada
        val completedAppt = Appointment(
            patientId = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
            startTime = LocalDateTime.of(2026, 10, 19, 8, 0),
            status = AppointmentStatus.COMPLETED
        )
        val completedJson = strictNullsJson.parseToJsonElement(strictNullsJson.encodeToString(completedAppt.toDto())).jsonObject
        assertEquals("COMPLETED", completedJson["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun databasePayloadRoundTripPreservesAllFields() {
        val original = Appointment(
            id = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
            patientId = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
            doctorId = "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33",
            originEvolutionId = "d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44",
            startTime = LocalDateTime.of(2026, 10, 20, 15, 30),
            endTime = LocalDateTime.of(2026, 10, 20, 16, 0),
            status = AppointmentStatus.CONFIRMED,
            isConfirmed = true,
            reason = "Ajuste de ortodoncia"
        )

        val serializedJson = supabaseSerializerJson.encodeToString(original.toDto())
        val deserializedDto = supabaseSerializerJson.decodeFromString<AppointmentDto>(serializedJson)
        val reconstructed = deserializedDto.toDomain()

        assertEquals(original.id, reconstructed.id)
        assertEquals(original.patientId, reconstructed.patientId)
        assertEquals(original.doctorId, reconstructed.doctorId)
        assertEquals(original.originEvolutionId, reconstructed.originEvolutionId)
        assertEquals(original.startTime, reconstructed.startTime)
        assertEquals(original.endTime, reconstructed.endTime)
        assertEquals(AppointmentStatus.CONFIRMED, reconstructed.status)
        assertEquals(true, reconstructed.isConfirmed)
        assertEquals(original.reason, reconstructed.reason)
    }

    @Test
    fun crossTableHeadersValidationForPatientAndDoctorEntities() {
        // 1. PatientDto headers validation
        val patient = Patient(
            id = "36b8f84d-df4e-4d49-b662-bcde71a8764f",
            userId = "  ", // debe sanitizarse a null
            dniCedula = "1020304050",
            firstName = "Valentina",
            lastName = "Gómez Martínez"
        )
        val patientDto = patient.toDto()
        assertNull("userId en blanco en Patient debe ser null", patientDto.userId)

        val patientJsonString = strictNullsJson.encodeToString(patientDto)
        val patientJsonObject = strictNullsJson.parseToJsonElement(patientJsonString).jsonObject
        assertTrue("Debe incluir header 'dni_cedula'", patientJsonObject.containsKey("dni_cedula"))
        assertTrue("Debe incluir header 'first_name'", patientJsonObject.containsKey("first_name"))
        assertTrue("Debe incluir header 'first_last_name'", patientJsonObject.containsKey("first_last_name"))
        assertTrue("Debe incluir header 'user_id'", patientJsonObject.containsKey("user_id"))
        assertTrue("user_id debe ser JsonNull", patientJsonObject["user_id"] is JsonNull)
        assertFalse("NO debe incluir camelCase dniCedula", patientJsonObject.containsKey("dniCedula"))
        assertFalse("NO debe incluir camelCase userId", patientJsonObject.containsKey("userId"))

        // 2. DoctorDto headers validation
        val doctorDto = DoctorDto(
            id = "550e8400-e29b-41d4-a716-446655440000",
            userId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
            firstName = "Dra. Laura",
            lastName = "Paredes",
            licenseNumber = "TP-998877",
            specialty = "Ortodoncia",
            bio = "Especialista en alineación",
            photoUrl = "https://example.com/photo.png"
        )
        val doctorJsonString = strictNullsJson.encodeToString(doctorDto)
        val doctorJsonObject = strictNullsJson.parseToJsonElement(doctorJsonString).jsonObject

        assertTrue("Debe incluir header 'user_id'", doctorJsonObject.containsKey("user_id"))
        assertTrue("Debe incluir header 'first_name'", doctorJsonObject.containsKey("first_name"))
        assertTrue("Debe incluir header 'last_name'", doctorJsonObject.containsKey("last_name"))
        assertTrue("Debe incluir header 'license_number'", doctorJsonObject.containsKey("license_number"))
        assertTrue("Debe incluir header 'photo_url'", doctorJsonObject.containsKey("photo_url"))
        assertFalse("NO debe contener 'licenseNumber'", doctorJsonObject.containsKey("licenseNumber"))
        assertFalse("NO debe contener 'photoUrl'", doctorJsonObject.containsKey("photoUrl"))

        // 3. DoctorAvailabilityDto headers validation
        val availabilityDto = DoctorAvailabilityDto(
            id = "avail-uuid-1",
            doctorId = "550e8400-e29b-41d4-a716-446655440000",
            dayOfWeek = 1,
            startTime = "08:00:00",
            endTime = "12:00:00",
            slotMinutes = 30,
            isActive = true
        )
        val availJsonString = strictNullsJson.encodeToString(availabilityDto)
        val availJsonObject = strictNullsJson.parseToJsonElement(availJsonString).jsonObject
        assertTrue("Debe incluir header 'doctor_id'", availJsonObject.containsKey("doctor_id"))
        assertTrue("Debe incluir header 'day_of_week'", availJsonObject.containsKey("day_of_week"))
        assertTrue("Debe incluir header 'start_time'", availJsonObject.containsKey("start_time"))
        assertTrue("Debe incluir header 'end_time'", availJsonObject.containsKey("end_time"))
        assertTrue("Debe incluir header 'slot_minutes'", availJsonObject.containsKey("slot_minutes"))
        assertTrue("Debe incluir header 'is_active'", availJsonObject.containsKey("is_active"))
        assertFalse("NO debe contener 'doctorId'", availJsonObject.containsKey("doctorId"))
        assertFalse("NO debe contener 'dayOfWeek'", availJsonObject.containsKey("dayOfWeek"))
    }
}
