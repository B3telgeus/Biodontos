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
    val upperLeftQuadrant: List<Int> get() = dentitionType.upperLeftQuadrant
    val upperRightQuadrant: List<Int> get() = dentitionType.upperRightQuadrant
    val lowerLeftQuadrant: List<Int> get() = dentitionType.lowerLeftQuadrant
    val lowerRightQuadrant: List<Int> get() = dentitionType.lowerRightQuadrant
}
