package com.example.biodontos.domain.model.billing

enum class TreatmentStatus(val displayName: String) {
    PENDIENTE("Pendiente"),
    APROBADO("Aprobado"),
    EN_CURSO("En Curso"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado");

    fun canTransitionTo(target: TreatmentStatus): Boolean = when (this) {
        PENDIENTE -> target == APROBADO || target == CANCELADO
        APROBADO -> target == EN_CURSO || target == CANCELADO
        EN_CURSO -> target == COMPLETADO || target == CANCELADO
        COMPLETADO, CANCELADO -> false
    }

    fun availableTransitions(): List<TreatmentStatus> =
        entries.filter { canTransitionTo(it) }

    companion object {
        fun fromString(raw: String): TreatmentStatus =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PENDIENTE
    }
}
