---
name: dental-usecase-generator
description: Genera Casos de Uso de Clean Architecture en Kotlin para la app odontológica utilizando Arrow-kt (Either) o Result funcional y Value Classes dentales (FDI). Usar al crear nueva lógica de negocio clínica pura.
---

# Generador de Casos de Uso Odontológicos (Clean Architecture)

Sigue este procedimiento para implementar un nuevo Caso de Uso en el módulo `:domain` (`com.example.biodontos.domain.usecase`).

---

## 1. Lista de Verificación de Arquitectura

1. **Pureza de Kotlin:** Cero importaciones de Android (`android.*`, `androidx.*`) en el paquete `domain`.
2. **Operador `invoke`:** La clase debe implementar `suspend operator fun invoke(...)`.
3. **Manejo Funcional de Errores (Sin Excepciones):** Prohibido el uso de `throw`. Utiliza `arrow.core.Either<DomainError, T>` (o `Result<T>` con `DomainError`) para modelar fallos de negocio explícitos.
4. **Validación Legal Bloqueante:** Toda acción clínica invasiva (marcar caries, exodoncia, plan de tratamiento) debe verificar el Consentimiento Informado antes de continuar.
5. **Value Classes (Primitive Obsession):** Encapsular identificadores en `@JvmInline value class ToothFdi(val value: Int)` y `@JvmInline value class PatientId(val value: String)`.

---

## 2. Taxonomía de Errores de Dominio (`DomainError`)

```kotlin
package com.example.biodontos.domain.model

sealed interface DomainError {
    data object ConsentNotGranted : DomainError
    data class InvalidToothNumber(val number: Int) : DomainError
    data class PatientNotFound(val patientId: String) : DomainError
    data class NetworkOrPersistenceFailure(val reason: String) : DomainError
}
```

---

## 3. Plantilla de Código Requerida

```kotlin
package com.example.biodontos.domain.usecase.odontogram

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.biodontos.domain.model.DomainError
import com.example.biodontos.domain.model.PatientId
import com.example.biodontos.domain.model.dental.ToothFdi
import com.example.biodontos.domain.model.dental.ToothSurface
import com.example.biodontos.domain.model.dental.ClinicalCondition
import com.example.biodontos.domain.repository.OdontogramRepository
import com.example.biodontos.domain.repository.PatientRepository

class UpdateToothConditionUseCase(
    private val odontogramRepository: OdontogramRepository,
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(
        patientId: PatientId,
        toothFdi: ToothFdi,
        surface: ToothSurface,
        condition: ClinicalCondition
    ): Either<DomainError, Unit> {
        // 1. Verificación obligatoria de consentimiento legal
        val patient = patientRepository.getPatientById(patientId.value)
            ?: return DomainError.PatientNotFound(patientId.value).left()

        if (!patient.consentSigned) {
            return DomainError.ConsentNotGranted.left()
        }

        // 2. Validación de rango FDI anatómico (Permanente: 11..48 | Deciduo: 51..85)
        if (!toothFdi.isValidFdi()) {
            return DomainError.InvalidToothNumber(toothFdi.value).left()
        }

        // 3. Persistencia delegada al repositorio
        return odontogramRepository.updateToothCondition(
            patientId = patientId.value,
            toothNumber = toothFdi.value,
            surface = surface,
            condition = condition
        ).fold(
            onSuccess = { Unit.right() },
            onFailure = { DomainError.NetworkOrPersistenceFailure(it.message ?: "Error desconocido").left() }
        )
    }
}
```
