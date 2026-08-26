package com.example.biodontos.presentation.odontogram

import com.example.biodontos.domain.model.Tooth
import com.example.biodontos.domain.model.ToothCondition
import com.example.biodontos.domain.model.ToothSurface

data class OdontogramState(
    val teeth: Map<Int, Tooth> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTooth: Int? = null,
    val selectedSurface: ToothSurface? = null,
    val showSelectionDialog: Boolean = false,
    val comment: String = "",
    val isSaving: Boolean = false
)
