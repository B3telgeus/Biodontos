package com.example.biodontos.domain.model

@JvmInline
value class PatientId(val value: String) {
    init {
        require(value.isNotBlank()) { "PatientId no puede ser vacío" }
    }
}

@JvmInline
value class DoctorId(val value: String) {
    init {
        require(value.isNotBlank()) { "DoctorId no puede ser vacío" }
    }
}

@JvmInline
value class AppointmentId(val value: String) {
    init {
        require(value.isNotBlank()) { "AppointmentId no puede ser vacío" }
    }
}

@JvmInline
value class MaterialId(val value: String) {
    init {
        require(value.isNotBlank()) { "MaterialId no puede ser vacío" }
    }

    companion object {
        fun generate(): MaterialId = MaterialId(java.util.UUID.randomUUID().toString())
    }
}

@JvmInline
value class CopAmount(val pesos: Long) {
    init {
        require(pesos >= 0) { "El monto en COP no puede ser negativo: $pesos" }
    }

    operator fun plus(other: CopAmount): CopAmount = CopAmount(this.pesos + other.pesos)
    operator fun minus(other: CopAmount): CopAmount = CopAmount((this.pesos - other.pesos).coerceAtLeast(0L))

    fun formatCop(): String {
        return "$ " + "%,d".format(pesos).replace(',', '.')
    }

    fun formatDisplay(): String = formatCop()

    companion object {
        val ZERO = CopAmount(0L)
    }
}
