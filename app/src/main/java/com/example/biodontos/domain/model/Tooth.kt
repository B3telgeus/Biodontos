package com.example.biodontos.domain.model

data class Tooth(
    val number: Int,
    val surfaceStates: Map<ToothSurface, ToothCondition>
) {
    init {
        val isPermanent = number in 11..18 || number in 21..28 || number in 31..38 || number in 41..48
        val isDeciduous = number in 51..55 || number in 61..65 || number in 71..75 || number in 81..85
        require(isPermanent || isDeciduous) { "Número dental FDI inválido: $number" }
    }
}

enum class ToothSurface { 
    VESTIBULAR, 
    OCLUSAL, 
    PALATINO, 
    MESIAL, 
    DISTAL 
}

enum class ToothCondition {
    SANO,
    CARIES,
    DESGASTE,
    CALZA,
    CORONILLA,
    TRATADO
}
