package com.example.biodontos.presentation.odontogram

import androidx.compose.runtime.Immutable
import com.example.biodontos.domain.model.DentitionType
import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothSurface

@Immutable
data class OdontogramState(
    val teeth: Map<Int, Tooth> = emptyMap(),
    val dentitionType: DentitionType = DentitionType.ADULT,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTooth: Int? = null,
    val selectedSurface: ToothSurface? = null,
    val showSelectionDialog: Boolean = false,
    val comment: String = "",
    val isSaving: Boolean = false
) {
    val upperLeftQuadrant: List<Int>
        get() = when (dentitionType) {
            DentitionType.ADULT -> listOf(18, 17, 16, 15, 14, 13, 12, 11)
            DentitionType.CHILD -> listOf(55, 54, 53, 52, 51)
            DentitionType.MIXED -> listOf(16, 55, 54, 53, 52, 11)
        }

    val upperRightQuadrant: List<Int>
        get() = when (dentitionType) {
            DentitionType.ADULT -> listOf(21, 22, 23, 24, 25, 26, 27, 28)
            DentitionType.CHILD -> listOf(61, 62, 63, 64, 65)
            DentitionType.MIXED -> listOf(21, 62, 63, 64, 65, 26)
        }

    val lowerLeftQuadrant: List<Int>
        get() = when (dentitionType) {
            DentitionType.ADULT -> listOf(48, 47, 46, 45, 44, 43, 42, 41)
            DentitionType.CHILD -> listOf(85, 84, 83, 82, 81)
            DentitionType.MIXED -> listOf(46, 85, 84, 83, 82, 41)
        }

    val lowerRightQuadrant: List<Int>
        get() = when (dentitionType) {
            DentitionType.ADULT -> listOf(31, 32, 33, 34, 35, 36, 37, 38)
            DentitionType.CHILD -> listOf(71, 72, 73, 74, 75)
            DentitionType.MIXED -> listOf(31, 72, 73, 74, 75, 36)
        }
}
