package com.example.biodontos.domain.model

sealed interface MaterialError : DomainError {
    val userMessage: String

    data object BlankName : MaterialError {
        override val userMessage: String = "El nombre del material es obligatorio."
    }

    data object BlankInvimaRegistration : MaterialError {
        override val userMessage: String = "El registro INVIMA es obligatorio."
    }

    data object BlankBatchLotNumber : MaterialError {
        override val userMessage: String = "El número de lote es obligatorio."
    }

    data object InvalidDates : MaterialError {
        override val userMessage: String = "La fecha de vencimiento debe ser posterior a la fecha de elaboración."
    }

    data object ManufacturingDateInFuture : MaterialError {
        override val userMessage: String = "La fecha de elaboración no puede ser una fecha futura."
    }

    data object MaterialExpired : MaterialError {
        override val userMessage: String = "El material seleccionado se encuentra vencido y no puede ser utilizado."
    }

    data object MaterialInactive : MaterialError {
        override val userMessage: String = "El material está inactivo y no puede ser utilizado en evoluciones."
    }

    data object InvalidQuantity : MaterialError {
        override val userMessage: String = "La cantidad utilizada debe ser mayor a 0."
    }

    data object DuplicateMaterial : MaterialError {
        override val userMessage: String = "Ya existe un material activo registrado con el mismo número de lote y registro INVIMA."
    }

    data object MaterialNotFound : MaterialError {
        override val userMessage: String = "El material solicitado no fue encontrado."
    }

    data class PersistenceError(val reason: String) : MaterialError {
        override val userMessage: String = "Error al persistir el material: $reason"
    }
}
