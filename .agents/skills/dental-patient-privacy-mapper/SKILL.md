---
name: dental-patient-privacy-mapper
description: Genera mappers de datos seguros entre DTOs (Open Dental / Supabase) y Dominio garantizando privacidad HIPAA/GDPR (cero PHI en logs, ofuscación y tipos seguros). Usar al integrar nuevos endpoints o entidades.
---

# Generador de Mappers Seguros y Privacidad HIPAA/GDPR

Esta Skill estandariza la transformación de datos entre la capa de Red/Persistencia (`data`) y el Dominio Clínico (`domain`), garantizando cumplimiento estricto de normativas de salud y protección de PHI (Protected Health Information).

---

## 1. Principios de Privacidad Sanitaria (HIPAA Compliance)

- **Cero PHI en Logs:** Prohibido registrar en `println`, `Logcat`, Timber o Crashlytics:
  - Nombres completos de pacientes.
  - Números de documento de identidad (DNI, Cédula, SSN).
  - Diagnósticos específicos o notas de evolución no anonimizadas.
- **Enmascaramiento de Identificadores:** Al depurar o trazar operaciones, enmascarar datos sensibles:
  ```kotlin
  fun maskDni(dni: String): String = if (dni.length > 4) "***" + dni.takeLast(4) else "***"
  ```
- **Aislamiento de Anotaciones:** Prohibido colocar anotaciones de serialización (`@SerialName`, `@Serializable`, `@Entity`) en las entidades del paquete `domain.model`.

---

## 2. Lista de Verificación del Mapper

- [ ] **Mapeo Bidireccional:** Crear `Dto.toDomain()` y `Domain.toDto()` como funciones de extensión.
- [ ] **Tipado Seguro de Fechas:** Transformar `String` ("yyyy-MM-dd HH:mm:ss") a `java.time.LocalDateTime` o `java.time.LocalDate`.
- [ ] **Manejo Defensivo de Nulos:** Asignar valores predeterminados seguros para campos opcionales de la API de Open Dental.
- [ ] **Value Classes:** Mapear identificadores primitivos a `PatientId`, `AppointmentId`, etc.

---

## 3. Plantilla de Código Requerida

```kotlin
package com.example.biodontos.data.remote.mapper

import com.example.biodontos.data.remote.dto.PatientDto
import com.example.biodontos.domain.model.Patient
import com.example.biodontos.domain.model.PatientId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun PatientDto.toDomain(): Patient {
    return Patient(
        id = PatientId(this.patNum.toString()),
        dniCedula = this.ssnOrDni.orEmpty(),
        firstName = this.fName,
        lastName = this.lName,
        birthDate = this.birthdate?.let { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() },
        hasCriticalAllergies = !this.medUrgNote.isNullOrBlank(),
        allergiesDescription = this.medUrgNote ?: "Sin alergias registradas",
        consentSigned = this.hasConsentSigned ?: false
    )
}

fun Patient.toDto(): PatientDto {
    return PatientDto(
        patNum = this.id.value.toLongOrNull(),
        fName = this.firstName,
        lName = this.lastName,
        ssnOrDni = this.dniCedula,
        birthdate = this.birthDate?.format(DATE_FORMATTER),
        medUrgNote = if (this.hasCriticalAllergies) this.allergiesDescription else null,
        hasConsentSigned = this.consentSigned
    )
}
```
