package com.example.biodontos.data.mapper

import com.example.biodontos.data.dto.AnamnesisDto
import com.example.biodontos.data.dto.ClinicalMaterialDto
import com.example.biodontos.data.dto.ClinicalRecordDto
import com.example.biodontos.data.dto.EvolutionDto
import com.example.biodontos.data.dto.EvolutionMaterialUsedDto
import com.example.biodontos.data.dto.InformedConsentDto
import com.example.biodontos.data.dto.InformedConsentProcedureDto
import com.example.biodontos.data.dto.PeriodontalExaminationDto
import com.example.biodontos.data.dto.PeriodontalSiteDto
import com.example.biodontos.domain.model.Anamnesis
import com.example.biodontos.domain.model.ClinicalMaterial
import com.example.biodontos.domain.model.ClinicalRecord
import com.example.biodontos.domain.model.ClinicalRecordStatus
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.Evolution
import com.example.biodontos.domain.model.EvolutionMaterialUsed
import com.example.biodontos.domain.model.InformedConsent
import com.example.biodontos.domain.model.InformedConsentProcedure
import com.example.biodontos.domain.model.PeriodontalExamination
import com.example.biodontos.domain.model.PeriodontalSiteLocation
import com.example.biodontos.domain.model.PeriodontalSiteMeasurement

// --- Anamnesis ---
fun AnamnesisDto.toDomain(): Anamnesis = Anamnesis(
    id = id ?: "",
    patientId = patientId,
    tratamientoMedicoActual = tratamientoMedicoActual,
    tomaMedicamentos = tomaMedicamentos,
    alergias = alergias,
    cardiopatias = cardiopatias,
    alteracionPresionArterial = alteracionPresionArterial,
    embarazo = embarazo,
    diabetes = diabetes,
    hepatitis = hepatitis,
    irradiaciones = irradiaciones,
    discrasiasSanguineas = discrasiasSanguineas,
    fiebreReumatica = fiebreReumatica,
    enfermedadesRenales = enfermedadesRenales,
    inmunosupresion = inmunosupresion,
    trastornosEmocionales = trastornosEmocionales,
    patologiaRespiratoria = patologiaRespiratoria,
    trastornosGastricos = trastornosGastricos,
    epilepsia = epilepsia,
    cirugias = cirugias,
    enfermedadesOrales = enfermedadesOrales,
    otrasAlteraciones = otrasAlteraciones,
    observaciones = observaciones,
    antecedentesFamiliares = antecedentesFamiliares,
    medicamentosActuales = medicamentosActuales,
    alergiasDescripcion = alergiasDescripcion
)

fun Anamnesis.toDto(): AnamnesisDto {
    val resolvedId = if (id.isNotBlank()) {
        id
    } else if (patientId.isNotBlank()) {
        java.util.UUID.nameUUIDFromBytes("anamnesis_$patientId".toByteArray(java.nio.charset.StandardCharsets.UTF_8)).toString()
    } else {
        null
    }
    return AnamnesisDto(
        id = resolvedId,
        patientId = patientId,
    tratamientoMedicoActual = tratamientoMedicoActual,
    tomaMedicamentos = tomaMedicamentos,
    alergias = alergias,
    cardiopatias = cardiopatias,
    alteracionPresionArterial = alteracionPresionArterial,
    embarazo = embarazo,
    diabetes = diabetes,
    hepatitis = hepatitis,
    irradiaciones = irradiaciones,
    discrasiasSanguineas = discrasiasSanguineas,
    fiebreReumatica = fiebreReumatica,
    enfermedadesRenales = enfermedadesRenales,
    inmunosupresion = inmunosupresion,
    trastornosEmocionales = trastornosEmocionales,
    patologiaRespiratoria = patologiaRespiratoria,
    trastornosGastricos = trastornosGastricos,
    epilepsia = epilepsia,
    cirugias = cirugias,
    enfermedadesOrales = enfermedadesOrales,
    otrasAlteraciones = otrasAlteraciones,
    observaciones = observaciones,
    antecedentesFamiliares = antecedentesFamiliares,
    medicamentosActuales = medicamentosActuales,
    alergiasDescripcion = alergiasDescripcion
)
}

// --- Clinical Record ---
fun ClinicalRecordDto.toDomain(): ClinicalRecord = ClinicalRecord(
    id = id ?: "",
    patientId = patientId,
    dentitionType = runCatching { DentitionType.valueOf(dentitionType) }.getOrDefault(DentitionType.ADULT),
    mainDiagnosis = mainDiagnosis,
    relatedDiagnoses = relatedDiagnoses,
    complementaryExams = complementaryExams,
    status = runCatching { ClinicalRecordStatus.valueOf(status) }.getOrDefault(ClinicalRecordStatus.ABIERTA),
    closedAt = closedAt,
    closedByDoctorId = closedByDoctorId,
    closureSummary = closureSummary,
    closureSignatureUrl = closureSignatureUrl
)

fun ClinicalRecord.toDto(): ClinicalRecordDto = ClinicalRecordDto(
    id = id.ifEmpty { null },
    patientId = patientId,
    dentitionType = dentitionType.name,
    mainDiagnosis = mainDiagnosis,
    relatedDiagnoses = relatedDiagnoses,
    complementaryExams = complementaryExams,
    status = status.name,
    closedAt = closedAt,
    closedByDoctorId = closedByDoctorId,
    closureSummary = closureSummary,
    closureSignatureUrl = closureSignatureUrl
)

// --- Periodontograma ---
fun PeriodontalExaminationDto.toDomain(sites: List<PeriodontalSiteMeasurement> = emptyList()): PeriodontalExamination =
    PeriodontalExamination(
        id = id ?: "",
        recordId = recordId,
        doctorId = doctorId,
        examinationDate = examinationDate,
        generalObservations = generalObservations,
        sites = sites
    )

fun PeriodontalExamination.toDto(): PeriodontalExaminationDto = PeriodontalExaminationDto(
    id = id.ifEmpty { null },
    recordId = recordId,
    doctorId = doctorId,
    examinationDate = examinationDate,
    generalObservations = generalObservations
)

fun PeriodontalSiteDto.toDomain(): PeriodontalSiteMeasurement = PeriodontalSiteMeasurement(
    toothNumber = toothNumber,
    siteLocation = runCatching { PeriodontalSiteLocation.valueOf(siteLocation) }.getOrDefault(PeriodontalSiteLocation.MID_VESTIBULAR),
    pocketDepthMm = pocketDepthMm,
    gingivalRecessionMm = gingivalRecessionMm,
    bleedingOnProbing = bleedingOnProbing,
    suppuration = suppuration,
    mobilityGrade = mobilityGrade.coerceIn(0, 3),
    furcationGrade = furcationGrade.coerceIn(0, 4)
)

fun PeriodontalSiteMeasurement.toDto(examinationId: String): PeriodontalSiteDto = PeriodontalSiteDto(
    id = null,
    examinationId = examinationId,
    toothNumber = toothNumber,
    siteLocation = siteLocation.name,
    pocketDepthMm = pocketDepthMm,
    gingivalRecessionMm = gingivalRecessionMm,
    clinicalAttachmentLossMm = clinicalAttachmentLossMm,
    bleedingOnProbing = bleedingOnProbing,
    suppuration = suppuration,
    mobilityGrade = mobilityGrade,
    furcationGrade = furcationGrade
)

// --- Evolution & Materials ---
fun EvolutionDto.toDomain(materials: List<EvolutionMaterialUsed> = emptyList()): Evolution = Evolution(
    id = id ?: "",
    recordId = recordId,
    doctorId = doctorId ?: "",
    date = date ?: "",
    procedureTime = procedureTime ?: "00:00:00",
    description = description,
    doctorName = doctorName,
    toothTreated = toothTreated,
    cost = cost,
    paymentAmount = paymentAmount,
    balance = (cost - paymentAmount).coerceAtLeast(0.0),
    patientSignatureUrl = patientSignatureUrl,
    doctorSignatureUrl = doctorSignatureUrl,
    materialsUsed = materials
)

fun Evolution.toDto(): EvolutionDto = EvolutionDto(
    id = id.ifEmpty { null },
    recordId = recordId,
    doctorId = doctorId.ifEmpty { null },
    date = date.ifEmpty { null },
    procedureTime = procedureTime.ifEmpty { null },
    description = description,
    doctorName = doctorName,
    toothTreated = toothTreated,
    cost = cost,
    paymentAmount = paymentAmount,
    balance = balance,
    patientSignatureUrl = patientSignatureUrl,
    doctorSignatureUrl = doctorSignatureUrl
)

fun EvolutionMaterialUsedDto.toDomain(): EvolutionMaterialUsed = EvolutionMaterialUsed(
    id = id ?: "",
    materialId = materialId,
    materialName = "",
    quantityUsed = quantityUsed,
    lotNumberSnapshot = lotNumberSnapshot,
    invimaSnapshot = invimaSnapshot,
    expirationDateSnapshot = expirationDateSnapshot
)

fun EvolutionMaterialUsed.toDto(evolutionId: String): EvolutionMaterialUsedDto = EvolutionMaterialUsedDto(
    id = id.ifEmpty { null },
    evolutionId = evolutionId,
    materialId = materialId,
    quantityUsed = quantityUsed,
    lotNumberSnapshot = lotNumberSnapshot,
    invimaSnapshot = invimaSnapshot,
    expirationDateSnapshot = expirationDateSnapshot
)

// --- Clinical Material ---
fun ClinicalMaterialDto.toDomain(): ClinicalMaterial = ClinicalMaterial(
    id = id ?: "",
    name = name,
    invimaRegistration = invimaRegistration,
    batchLotNumber = batchLotNumber,
    manufacturingDate = manufacturingDate,
    expirationDate = expirationDate,
    currentStock = currentStock,
    unitOfMeasure = unitOfMeasure
)

fun ClinicalMaterial.toDto(): ClinicalMaterialDto = ClinicalMaterialDto(
    id = id.ifEmpty { null },
    name = name,
    invimaRegistration = invimaRegistration,
    batchLotNumber = batchLotNumber,
    manufacturingDate = manufacturingDate,
    expirationDate = expirationDate,
    currentStock = currentStock,
    unitOfMeasure = unitOfMeasure
)

// --- Informed Consent ---
fun InformedConsentDto.toDomain(procedures: List<InformedConsentProcedure> = emptyList()): InformedConsent =
    InformedConsent(
        id = id ?: "",
        patientId = patientId,
        doctorId = doctorId,
        documentVersion = documentVersion,
        versionDate = versionDate,
        treatmentSummary = treatmentSummary,
        representativeName = representativeName,
        representativeId = representativeId,
        patientSignatureUrl = patientSignatureUrl,
        guardianSignatureUrl = guardianSignatureUrl,
        doctorSignatureUrl = doctorSignatureUrl,
        doctorLicenseNumber = doctorLicenseNumber,
        signedAt = signedAt,
        status = status,
        procedures = procedures
    )

fun InformedConsent.toDto(): InformedConsentDto = InformedConsentDto(
    id = id.ifEmpty { null },
    patientId = patientId,
    doctorId = doctorId,
    documentVersion = documentVersion,
    versionDate = versionDate,
    treatmentSummary = treatmentSummary,
    representativeName = representativeName,
    representativeId = representativeId,
    patientSignatureUrl = patientSignatureUrl,
    guardianSignatureUrl = guardianSignatureUrl,
    doctorSignatureUrl = doctorSignatureUrl,
    doctorLicenseNumber = doctorLicenseNumber,
    signedAt = signedAt,
    status = status
)

fun InformedConsentProcedureDto.toDomain(): InformedConsentProcedure = InformedConsentProcedure(
    id = id ?: "",
    consentId = consentId,
    toothNumber = toothNumber,
    procedureDate = procedureDate,
    patientSignatureUrl = patientSignatureUrl
)

fun InformedConsentProcedure.toDto(consentId: String = this.consentId): InformedConsentProcedureDto = InformedConsentProcedureDto(
    id = id.ifEmpty { null },
    consentId = consentId,
    toothNumber = toothNumber,
    procedureDate = procedureDate,
    patientSignatureUrl = patientSignatureUrl
)
