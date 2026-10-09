package com.example.biodontos.domain.model

enum class DentitionType {
    ADULT,
    CHILD,
    MIXED
}

enum class ClinicalRecordStatus {
    ABIERTA,
    EN_TRATAMIENTO,
    CERRADA,
    ARCHIVADA
}

data class ClinicalRecord(
    val id: String = "",
    val patientId: String,
    val dentitionType: DentitionType = DentitionType.ADULT,
    val mainDiagnosis: String? = null,
    val relatedDiagnoses: String? = null,
    val complementaryExams: String? = null,
    val status: ClinicalRecordStatus = ClinicalRecordStatus.ABIERTA,
    val closedAt: String? = null,
    val closedByDoctorId: String? = null,
    val closureSummary: String? = null,
    val closureSignatureUrl: String? = null
)
