package com.example.biodontos.domain.model

enum class DentitionType {
    ADULT,
    CHILD,
    MIXED;

    val upperLeftQuadrant: List<Int>
        get() = when (this) {
            ADULT -> listOf(18, 17, 16, 15, 14, 13, 12, 11)
            CHILD -> listOf(55, 54, 53, 52, 51)
            MIXED -> listOf(16, 55, 54, 53, 52, 11)
        }

    val upperRightQuadrant: List<Int>
        get() = when (this) {
            ADULT -> listOf(21, 22, 23, 24, 25, 26, 27, 28)
            CHILD -> listOf(61, 62, 63, 64, 65)
            MIXED -> listOf(21, 62, 63, 64, 65, 26)
        }

    val lowerLeftQuadrant: List<Int>
        get() = when (this) {
            ADULT -> listOf(48, 47, 46, 45, 44, 43, 42, 41)
            CHILD -> listOf(85, 84, 83, 82, 81)
            MIXED -> listOf(46, 85, 84, 83, 82, 41)
        }

    val lowerRightQuadrant: List<Int>
        get() = when (this) {
            ADULT -> listOf(31, 32, 33, 34, 35, 36, 37, 38)
            CHILD -> listOf(71, 72, 73, 74, 75)
            MIXED -> listOf(31, 72, 73, 74, 75, 36)
        }

    val allTeeth: List<Int>
        get() = upperLeftQuadrant + upperRightQuadrant + lowerLeftQuadrant + lowerRightQuadrant

    companion object {
        val ADULT_FDI_TEETH: List<Int> = ADULT.allTeeth
    }
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
    val closureSignatureUrl: String? = null,
    val openedAt: String? = null
)
