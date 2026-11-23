package com.example.biodontos.domain.model

enum class PeriodontalSiteLocation {
    MESIO_VESTIBULAR,
    MID_VESTIBULAR,
    DISTO_VESTIBULAR,
    MESIO_LINGUAL,
    MID_LINGUAL,
    DISTO_LINGUAL;

    fun shortLabelForTooth(toothNumber: Int): String {
        val isUpper = isUpperArchTooth(toothNumber)
        return when (this) {
            MESIO_VESTIBULAR -> "MV"
            MID_VESTIBULAR -> "V"
            DISTO_VESTIBULAR -> "DV"
            MESIO_LINGUAL -> if (isUpper) "MP" else "ML"
            MID_LINGUAL -> if (isUpper) "P" else "L"
            DISTO_LINGUAL -> if (isUpper) "DP" else "DL"
        }
    }

    fun fullLabelForTooth(toothNumber: Int): String {
        val isUpper = isUpperArchTooth(toothNumber)
        return when (this) {
            MESIO_VESTIBULAR -> "Mesiovestibular"
            MID_VESTIBULAR -> "Vestibular medio"
            DISTO_VESTIBULAR -> "Distovestibular"
            MESIO_LINGUAL -> if (isUpper) "Mesiopalatino" else "Mesiolingual"
            MID_LINGUAL -> if (isUpper) "Palatino medio" else "Lingual medio"
            DISTO_LINGUAL -> if (isUpper) "Distopalatino" else "Distolingual"
        }
    }

    companion object {
        fun isUpperArchTooth(toothNumber: Int): Boolean {
            val quadrant = toothNumber / 10
            return quadrant == 1 || quadrant == 2 || quadrant == 5 || quadrant == 6
        }

        fun isLowerArchTooth(toothNumber: Int): Boolean {
            val quadrant = toothNumber / 10
            return quadrant == 3 || quadrant == 4 || quadrant == 7 || quadrant == 8
        }

        fun innerFaceTitleForTooth(toothNumber: Int): String =
            if (isUpperArchTooth(toothNumber)) "CARA PALATINA" else "CARA LINGUAL"
    }
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
