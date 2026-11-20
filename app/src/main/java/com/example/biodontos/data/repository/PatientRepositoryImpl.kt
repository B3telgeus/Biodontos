package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient as AppSupabaseClient
import com.example.biodontos.data.dto.PatientDto
import com.example.biodontos.data.local.dao.PatientDao
import com.example.biodontos.data.local.entity.PatientEntity
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.repository.PatientRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PatientRepositoryImpl(
    private val supabaseClient: SupabaseClient = AppSupabaseClient.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val patientDao: PatientDao? = null,
    internal var remotePatientsFetcher: (suspend () -> List<PatientDto>)? = null,
    internal var remotePatientByIdFetcher: (suspend (String) -> PatientDto?)? = null
) : PatientRepository {

    override suspend fun getPatients(): Result<List<Patient>> = withContext(ioDispatcher) {
        try {
            val result = remotePatientsFetcher?.invoke()
                ?: supabaseClient.postgrest.from("patients").select().decodeList<PatientDto>()
            val domainList = result.map { it.toDomain() }
            patientDao?.let { dao ->
                val entities = domainList.filter { it.id.isNotBlank() }.map { it.toEntity() }
                if (entities.isNotEmpty()) {
                    dao.insertAll(entities)
                }
            }
            Result.success(domainList)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val cached = patientDao?.let { dao ->
                try {
                    dao.searchPatients("").map { it.toDomain() }
                } catch (cacheEx: Exception) {
                    if (cacheEx is CancellationException) throw cacheEx
                    emptyList()
                }
            }.orEmpty()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPatientById(id: String): Result<Patient?> = withContext(ioDispatcher) {
        try {
            val result = remotePatientByIdFetcher?.invoke(id)
                ?: supabaseClient.postgrest.from("patients")
                    .select { filter { eq("id", id) } }
                    .decodeSingleOrNull<PatientDto>()
            val domain = result?.toDomain()
            if (domain != null && domain.id.isNotBlank()) {
                patientDao?.insert(domain.toEntity())
            }
            Result.success(domain)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val cached = patientDao?.let { dao ->
                try {
                    dao.getPatientById(id)?.toDomain()
                } catch (cacheEx: Exception) {
                    if (cacheEx is CancellationException) throw cacheEx
                    null
                }
            }
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPatientByUserId(userId: String): Result<Patient?> = withContext(ioDispatcher) {
        try {
            val result = supabaseClient.postgrest.from("patients")
                .select {
                    filter {
                        or {
                            eq("user_id", userId)
                            eq("id", userId)
                        }
                    }
                }
                .decodeSingleOrNull<PatientDto>()
            val domain = result?.toDomain()
            if (domain != null && domain.id.isNotBlank()) {
                patientDao?.insert(domain.toEntity())
            }
            Result.success(domain)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun savePatient(patient: Patient): Result<Unit> = withContext(ioDispatcher) {
        try {
            supabaseClient.postgrest.from("patients").upsert(patient.toDto())
            if (patient.id.isNotBlank()) {
                patientDao?.insert(patient.toEntity())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun createOrUpdatePatient(patient: Patient): Result<Patient> = withContext(ioDispatcher) {
        try {
            val dto = patient.toDto()
            val savedDto = supabaseClient.postgrest.from("patients").upsert(dto) {
                select()
            }.decodeSingle<PatientDto>()
            val savedDomain = savedDto.toDomain()
            if (savedDomain.id.isNotBlank()) {
                patientDao?.insert(savedDomain.toEntity())
            }
            Result.success(savedDomain)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}

private fun Patient.toEntity(): PatientEntity = PatientEntity(
    id = id,
    dniCedula = dniCedula,
    firstName = firstName,
    firstLastName = firstLastName,
    secondLastName = secondLastName.ifBlank { null },
    phone = phone,
    email = email,
    birthDate = birthDate.ifBlank { null },
    birthPlace = birthPlace.ifBlank { null },
    eps = eps.ifBlank { null },
    bloodGroup = bloodGroup.ifBlank { null },
    rhFactor = rhFactor.ifBlank { null },
    consentSigned = consentSigned
)

private fun PatientEntity.toDomain(): Patient = Patient(
    id = id,
    dniCedula = dniCedula,
    firstName = firstName,
    firstLastName = firstLastName,
    secondLastName = secondLastName.orEmpty(),
    phone = phone,
    email = email,
    birthDate = birthDate.orEmpty(),
    birthPlace = birthPlace ?: "No especificado",
    eps = eps ?: "Particular",
    bloodGroup = bloodGroup ?: "O",
    rhFactor = rhFactor ?: "+",
    consentSigned = consentSigned
)
