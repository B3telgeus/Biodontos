package com.example.biodontos.domain.model

enum class PeriodontalSiteLocation {
    MESIO_VESTIBULAR,
    MID_VESTIBULAR,
    DISTO_VESTIBULAR,
    MESIO_LINGUAL,
    MID_LINGUAL,
    DISTO_LINGUAL
}

data class PeriodontalSiteMeasurement(
    val toothNumber: Int,
    val siteLocation: PeriodontalSiteLocation,
    val pocketDepthMm: Float = 1.0f,
    val gingivalRecessionMm: Float = 0.0f,
    val bleedingOnProbing: Boolean = false,
    val suppuration: Boolean = false,
    val mobilityGrade: Int = 0,
    val furcationGrade: Int = 0
) {
    val clinicalAttachmentLossMm: Float get() = pocketDepthMm + gingivalRecessionMm
    val isDeepPocket: Boolean get() = pocketDepthMm >= 4.0f

    init {
        require(pocketDepthMm >= 0f) { "La profundidad de bolsa no puede ser negativa" }
        require(mobilityGrade in 0..3) { "Grado de movilidad inválido (0..3)" }
        require(furcationGrade in 0..4) { "Grado de furca inválido (0..4)" }
    }
}

data class PeriodontalExamination(
    val id: String = "",
    val recordId: String,
    val doctorId: String,
    val examinationDate: String,
    val generalObservations: String? = null,
    val sites: List<PeriodontalSiteMeasurement> = emptyList()
)
