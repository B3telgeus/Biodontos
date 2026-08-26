package com.example.biodontos.domain.model

import androidx.compose.ui.graphics.Color

data class Tooth(
    val number: Int,
    val surfaceStates: Map<ToothSurface, ToothCondition>
)

enum class ToothSurface { VESTIBULAR, OCLUSAL, PALATINO, MESIAL, DISTAL }

enum class ToothCondition(val color: Color) {
    SANO(Color(0xFFCBD5E1)),
    CARIES(Color(0xFFDC2626)),
    DESGASTE(Color(0xFFFACC15)),
    CALZA(Color(0xFF3B82F6)),
    CORONILLA(Color(0xFF8B5CF6)),
    TRATADO(Color(0xFF10B981))
}
