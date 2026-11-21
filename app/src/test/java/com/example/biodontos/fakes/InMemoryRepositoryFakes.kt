package com.example.biodontos.fakes

import com.example.biodontos.domain.model.Appointment
import com.example.biodontos.domain.model.AppointmentStatus
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.AppointmentRepository
import com.example.biodontos.domain.repository.MaterialRepository
import com.example.biodontos.domain.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class FakePatientRepository : PatientRepository {
    private val lock = Any()
    private val storage = LinkedHashMap<String, Patient>()
    private val _patientsFlow = MutableStateFlow<List<Patient>>(emptyList())
    val patientsFlow: StateFlow<List<Patient>> = _patientsFlow.asStateFlow()

    private var pendingFailure: Throwable? = null

    fun seed(vararg patients: Patient) {
        synchronized(lock) {
            patients.forEach { patient ->
                val resolved = ensurePatientId(patient)
                storage[resolved.id] = resolved
            }
            _patientsFlow.value = storage.values.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            storage.clear()
            pendingFailure = null
            _patientsFlow.value = emptyList()
        }
    }

    fun failNextWith(error: Throwable) {
        synchronized(lock) {
            pendingFailure = error
        }
    }

    override suspend fun getPatients(): Result<List<Patient>> = synchronized(lock) {
        consumeFailure<List<Patient>>()?.let { return it }
        Result.success(storage.values.toList())
    }

    override suspend fun getPatientById(id: String): Result<Patient?> = synchronized(lock) {
        consumeFailure<Patient?>()?.let { return it }
        Result.success(storage[id])
    }

    override suspend fun getPatientByUserId(userId: String): Result<Patient?> = synchronized(lock) {
        consumeFailure<Patient?>()?.let { return it }
        Result.success(storage.values.firstOrNull { it.userId == userId || it.id == userId })
    }

    override suspend fun savePatient(patient: Patient): Result<Unit> = synchronized(lock) {
        consumeFailure<Unit>()?.let { return it }
        val resolved = ensurePatientId(patient)
        storage[resolved.id] = resolved
        _patientsFlow.value = storage.values.toList()
        Result.success(Unit)
    }

    override suspend fun createOrUpdatePatient(patient: Patient): Result<Patient> = synchronized(lock) {
        consumeFailure<Patient>()?.let { return it }
        val resolved = ensurePatientId(patient)
        storage[resolved.id] = resolved
        _patientsFlow.value = storage.values.toList()
        Result.success(resolved)
    }

    private fun ensurePatientId(patient: Patient): Patient {
        if (patient.id.isNotBlank()) return patient
        return patient.copy(id = Patient.generateIdFromDni(patient.dniCedula))
    }

    private fun <T> consumeFailure(): Result<T>? {
        val failure = pendingFailure ?: return null
        pendingFailure = null
        return Result.failure(failure)
    }
}

class FakeAppointmentRepository : AppointmentRepository {
    private val lock = Any()
    private val storage = LinkedHashMap<String, Appointment>()
    private val _appointmentsFlow = MutableStateFlow<List<Appointment>>(emptyList())
    val appointmentsFlow: StateFlow<List<Appointment>> = _appointmentsFlow.asStateFlow()

    private var pendingFailure: Throwable? = null

    fun seed(vararg appointments: Appointment) {
        synchronized(lock) {
            appointments.forEach { appt ->
                val resolved = if (appt.id.isBlank()) {
                    appt.copy(id = UUID.randomUUID().toString())
                } else {
                    appt
                }
                storage[resolved.id] = resolved
            }
            _appointmentsFlow.value = storage.values.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            storage.clear()
            pendingFailure = null
            _appointmentsFlow.value = emptyList()
        }
    }

    fun failNextWith(error: Throwable) {
        synchronized(lock) {
            pendingFailure = error
        }
    }

    override suspend fun updateConfirmationStatus(appointmentId: String, isConfirmed: Boolean): Result<Unit> =
        synchronized(lock) {
            consumeFailure<Unit>()?.let { return it }
            val existing = storage[appointmentId]
                ?: return Result.failure(NoSuchElementException("Cita no encontrada: $appointmentId"))
            storage[appointmentId] = existing.copy(isConfirmed = isConfirmed)
            _appointmentsFlow.value = storage.values.toList()
            Result.success(Unit)
        }

    override suspend fun createAppointment(appointment: Appointment): Result<Appointment> =
        synchronized(lock) {
            consumeFailure<Appointment>()?.let { return it }
            val resolved = if (appointment.id.isBlank()) {
                appointment.copy(id = UUID.randomUUID().toString())
            } else {
                appointment
            }
            val newEnd = resolved.endTime ?: resolved.startTime.plusMinutes(30)
            val hasOverlap = resolved.doctorId != null && storage.values.any { existing ->
                existing.id != resolved.id &&
                    existing.doctorId == resolved.doctorId &&
                    existing.status != AppointmentStatus.CANCELLED &&
                    intervalsOverlap(
                        startA = existing.startTime,
                        endA = existing.endTime ?: existing.startTime.plusMinutes(30),
                        startB = resolved.startTime,
                        endB = newEnd
                    )
            }
            if (hasOverlap) {
                return Result.failure(IllegalStateException("El horario ya está ocupado para el especialista."))
            }
            storage[resolved.id] = resolved
            _appointmentsFlow.value = storage.values.toList()
            Result.success(resolved)
        }

    override suspend fun getAppointmentsByPatientId(patientId: String): Result<List<Appointment>> =
        synchronized(lock) {
            consumeFailure<List<Appointment>>()?.let { return it }
            Result.success(
                storage.values
                    .filter { it.patientId == patientId }
                    .sortedBy { it.startTime }
            )
        }

    override suspend fun getAppointmentsByDoctorId(doctorId: String): Result<List<Appointment>> =
        synchronized(lock) {
            consumeFailure<List<Appointment>>()?.let { return it }
            Result.success(
                storage.values
                    .filter { it.doctorId == doctorId }
                    .sortedBy { it.startTime }
            )
        }

    override suspend fun getAppointmentById(appointmentId: String): Result<Appointment?> =
        synchronized(lock) {
            consumeFailure<Appointment?>()?.let { return it }
            Result.success(storage[appointmentId])
        }

    override suspend fun getDoctorBusySlots(
        doctorId: String,
        from: LocalDateTime,
        to: LocalDateTime
    ): Result<List<ClosedRange<LocalDateTime>>> = synchronized(lock) {
        consumeFailure<List<ClosedRange<LocalDateTime>>>()?.let { return it }
        val slots = storage.values
            .filter { appt ->
                appt.doctorId == doctorId &&
                    appt.status != AppointmentStatus.CANCELLED &&
                    !appt.startTime.isBefore(from) &&
                    !appt.startTime.isAfter(to)
            }
            .sortedBy { it.startTime }
            .map { appt ->
                val end = appt.endTime ?: appt.startTime.plusMinutes(30)
                appt.startTime..end
            }
        Result.success(slots)
    }

    override suspend fun getDoctorBusySlots(doctorId: String, date: String): Result<List<String>> =
        synchronized(lock) {
            consumeFailure<List<String>>()?.let { return it }
            val slots = storage.values
                .filter { appt ->
                    appt.doctorId == doctorId &&
                        appt.status != AppointmentStatus.CANCELLED &&
                        appt.startTime.toLocalDate().toString() == date
                }
                .sortedBy { it.startTime }
                .map { it.startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
            Result.success(slots)
        }

    override suspend fun getAppointmentsByDoctorAndDate(doctorId: String, date: String): Result<List<Appointment>> =
        synchronized(lock) {
            consumeFailure<List<Appointment>>()?.let { return it }
            val matches = storage.values
                .filter { appt ->
                    appt.doctorId == doctorId &&
                        appt.status != AppointmentStatus.CANCELLED &&
                        appt.startTime.toLocalDate().toString() == date
                }
                .sortedBy { it.startTime }
            Result.success(matches)
        }

    override suspend fun rescheduleAppointment(
        appointmentId: String,
        newStartTime: LocalDateTime,
        newEndTime: LocalDateTime
    ): Result<Unit> = synchronized(lock) {
        consumeFailure<Unit>()?.let { return it }
        val existing = storage[appointmentId]
            ?: return Result.failure(NoSuchElementException("Cita no encontrada: $appointmentId"))
        storage[appointmentId] = existing.copy(
            startTime = newStartTime,
            endTime = newEndTime,
            isConfirmed = false,
            status = AppointmentStatus.SCHEDULED
        )
        _appointmentsFlow.value = storage.values.toList()
        Result.success(Unit)
    }

    override suspend fun rescheduleAppointment(
        appointmentId: String,
        newStartTime: String,
        newEndTime: String?
    ): Result<Unit> {
        val parsedStart = Appointment.parseDateTime(newStartTime)
        val parsedEnd = newEndTime?.let { Appointment.parseDateTime(it) } ?: parsedStart.plusMinutes(30)
        return rescheduleAppointment(appointmentId, parsedStart, parsedEnd)
    }

    override suspend fun cancelAppointment(appointmentId: String, reason: String?): Result<Unit> =
        synchronized(lock) {
            consumeFailure<Unit>()?.let { return it }
            val existing = storage[appointmentId]
                ?: return Result.failure(NoSuchElementException("Cita no encontrada: $appointmentId"))
            storage[appointmentId] = existing.copy(
                status = AppointmentStatus.CANCELLED,
                isConfirmed = false,
                reason = reason?.takeIf { it.isNotBlank() } ?: existing.reason
            )
            _appointmentsFlow.value = storage.values.toList()
            Result.success(Unit)
        }

    private fun intervalsOverlap(
        startA: LocalDateTime,
        endA: LocalDateTime,
        startB: LocalDateTime,
        endB: LocalDateTime
    ): Boolean = startA.isBefore(endB) && startB.isBefore(endA)

    private fun <T> consumeFailure(): Result<T>? {
        val failure = pendingFailure ?: return null
        pendingFailure = null
        return Result.failure(failure)
    }
}

class FakeMaterialRepository : MaterialRepository {
    private val lock = Any()
    private val storage = LinkedHashMap<String, ClinicalMaterial>()
    private val _materialsFlow = MutableStateFlow<List<ClinicalMaterial>>(emptyList())
    val materialsFlow: StateFlow<List<ClinicalMaterial>> = _materialsFlow.asStateFlow()

    private var pendingFailure: Throwable? = null

    fun seed(vararg materials: ClinicalMaterial) {
        synchronized(lock) {
            materials.forEach { material ->
                storage[material.id.value] = material
            }
            _materialsFlow.value = storage.values.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            storage.clear()
            pendingFailure = null
            _materialsFlow.value = emptyList()
        }
    }

    fun failNextWith(error: Throwable) {
        synchronized(lock) {
            pendingFailure = error
        }
    }

    override suspend fun getMaterials(
        includeInactive: Boolean,
        doctorId: DoctorId?
    ): Result<List<ClinicalMaterial>> = synchronized(lock) {
        consumeFailure<List<ClinicalMaterial>>()?.let { return it }
        val result = storage.values
            .filter { material ->
                (includeInactive || material.isActive) &&
                    (doctorId == null || material.doctorId == doctorId)
            }
            .sortedBy { it.name.lowercase() }
        Result.success(result)
    }

    override suspend fun getMaterialById(id: MaterialId): Result<ClinicalMaterial?> = synchronized(lock) {
        consumeFailure<ClinicalMaterial?>()?.let { return it }
        Result.success(storage[id.value])
    }

    override suspend fun saveMaterial(material: ClinicalMaterial): Result<ClinicalMaterial> = synchronized(lock) {
        consumeFailure<ClinicalMaterial>()?.let { return it }
        val isDuplicateActive = material.isActive && storage.values.any { existing ->
            existing.id != material.id &&
                existing.isActive &&
                existing.invimaRegistration.equals(material.invimaRegistration, ignoreCase = true) &&
                existing.batchLotNumber.equals(material.batchLotNumber, ignoreCase = true)
        }
        if (isDuplicateActive) {
            return Result.failure(IllegalStateException("ux_clinical_materials: duplicate key"))
        }
        storage[material.id.value] = material
        _materialsFlow.value = storage.values.toList()
        Result.success(material)
    }

    override suspend fun setMaterialActive(id: MaterialId, isActive: Boolean): Result<Unit> = synchronized(lock) {
        consumeFailure<Unit>()?.let { return it }
        val existing = storage[id.value]
            ?: return Result.failure(NoSuchElementException("Material no encontrado: ${id.value}"))
        storage[id.value] = existing.copy(isActive = isActive)
        _materialsFlow.value = storage.values.toList()
        Result.success(Unit)
    }

    private fun <T> consumeFailure(): Result<T>? {
        val failure = pendingFailure ?: return null
        pendingFailure = null
        return Result.failure(failure)
    }
}
