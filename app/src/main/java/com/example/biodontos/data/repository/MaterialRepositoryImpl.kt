package com.example.biodontos.data.repository

import com.example.biodontos.core.network.SupabaseClient as AppSupabaseClient
import com.example.biodontos.data.dto.ClinicalMaterialDto
import com.example.biodontos.data.local.dao.MaterialDao
import com.example.biodontos.data.local.entity.MaterialEntity
import com.example.biodontos.data.mapper.toDomain
import com.example.biodontos.data.mapper.toDto
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.DoctorId
import com.example.biodontos.domain.model.MaterialId
import com.example.biodontos.domain.repository.MaterialRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MaterialRepositoryImpl(
    private val supabaseClient: SupabaseClient = AppSupabaseClient.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val materialDao: MaterialDao? = null,
    internal var remoteMaterialsFetcher: (suspend (Boolean, DoctorId?) -> List<ClinicalMaterialDto>)? = null
) : MaterialRepository {

    override suspend fun getMaterials(
        includeInactive: Boolean,
        doctorId: DoctorId?
    ): Result<List<ClinicalMaterial>> = withContext(ioDispatcher) {
        try {
            val list = remoteMaterialsFetcher?.invoke(includeInactive, doctorId)
                ?: supabaseClient.postgrest.from("clinical_materials")
                    .select {
                        filter {
                            if (!includeInactive) {
                                eq("is_active", true)
                            }
                            if (doctorId != null) {
                                eq("doctor_id", doctorId.value)
                            }
                        }
                        order("name", order = Order.ASCENDING)
                    }
                    .decodeList<ClinicalMaterialDto>()
            val domainList = list.map { it.toDomain() }
            materialDao?.let { dao ->
                val entities = domainList.map { it.toEntity() }
                if (entities.isNotEmpty()) {
                    dao.insertAll(entities)
                }
            }
            Result.success(domainList)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val cached = materialDao?.let { dao ->
                try {
                    dao.getActiveMaterials()
                        .filter { doctorId == null || it.doctorId == doctorId.value }
                        .map { it.toDomain() }
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

    override suspend fun getMaterialById(id: MaterialId): Result<ClinicalMaterial?> = withContext(ioDispatcher) {
        try {
            val dto = supabaseClient.postgrest.from("clinical_materials")
                .select {
                    filter {
                        eq("id", id.value)
                    }
                }
                .decodeSingleOrNull<ClinicalMaterialDto>()
            Result.success(dto?.toDomain())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun saveMaterial(material: ClinicalMaterial): Result<ClinicalMaterial> = withContext(ioDispatcher) {
        try {
            val dto = material.toDto()
            val saved = supabaseClient.postgrest.from("clinical_materials")
                .upsert(dto) {
                    select()
                }
                .decodeSingle<ClinicalMaterialDto>()
            val savedDomain = saved.toDomain()
            materialDao?.insertAll(listOf(savedDomain.toEntity()))
            Result.success(savedDomain)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun setMaterialActive(id: MaterialId, isActive: Boolean): Result<Unit> = withContext(ioDispatcher) {
        try {
            val nowIso = Instant.now().toString()
            supabaseClient.postgrest.from("clinical_materials")
                .update(
                    {
                        set("is_active", isActive)
                        set("updated_at", nowIso)
                    }
                ) {
                    filter {
                        eq("id", id.value)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}

private fun ClinicalMaterial.toEntity(): MaterialEntity = MaterialEntity(
    id = id.value,
    doctorId = doctorId?.value,
    name = name,
    invimaRegistration = invimaRegistration,
    batchLotNumber = batchLotNumber,
    manufacturingDate = manufacturingDate.toString(),
    expirationDate = expirationDate.toString(),
    isActive = isActive
)

private fun MaterialEntity.toDomain(): ClinicalMaterial = ClinicalMaterial(
    id = id,
    doctorId = doctorId,
    name = name,
    invimaRegistration = invimaRegistration,
    batchLotNumber = batchLotNumber,
    manufacturingDate = manufacturingDate,
    expirationDate = expirationDate,
    isActive = isActive
)
