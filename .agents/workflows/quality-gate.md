# WORKFLOW: /quality-gate (Auditoría de Arquitectura, Detekt y HIPAA)

Este pipeline de verificación automatizada audita el código generado antes de integrarlo, realizar commits o fusionar pull requests en **Biodontos**.

---

## Fases del Pipeline Quality Gate

```text
 1. Pureza Arquitectónica ──> 2. Manejo de Errores ──> 3. Privacidad HIPAA ──> 4. Tests & Compilación
    (0 imports Android)          (Sin 'throw', Either)    (0 PHI en logs)        (./gradlew test)
```

---

## 1. Fase 1 — Chequeo de Aislamiento de Capas (Pureza de Dominio)
- **Objetivo:** Garantizar que la capa de negocio sea Kotlin 100% agnóstico de frameworks.
- **Ejecución:**
  ```powershell
  powershell -ExecutionPolicy Bypass -File .agents/skills/quality-detekt-verify/scripts/verify-domain-purity.ps1
  ```
- **Criterio de Aprobación:** 0 importaciones de `android.*` o `androidx.*` en `app/src/main/java/com/example/biodontos/domain`.

---

## 2. Fase 2 — Auditoría de Manejo Funcional de Errores
- **Objetivo:** Prevenir excepciones no controladas y crashes en tiempo de ejecución.
- **Verificación:**
  - Buscar que ningún Caso de Uso lance excepciones con `throw`:
    - ❌ Prohibido: `throw IllegalStateException(...)` o `throw DentalException(...)`
    - ✅ Obligatorio: `Either.Left(DomainError)` o `Result.failure(error)`
  - Confirmar que la capa de Presentación consuma los errores mediante `.fold()` o `.onFailure()` emitiendo efectos amigables al usuario (Toasts o diálogos) sin mostrar trazas de error técnico.

---

## 3. Fase 3 — Auditoría de Seguridad Sanitaria y Privacidad (HIPAA)
- **Objetivo:** Cumplir las leyes de privacidad médica internacional.
- **Verificación:**
  - Auditar que ningún archivo use `Log.d`, `Log.e`, `println` o `Timber` imprimiendo variables de pacientes (`patient.firstName`, `patient.dniCedula`, notas de anamnesis).
  - Los identificadores clínicos en logs deben estar anonimizados o enmascarados (`***1234`).
  - Las entidades de Room locales deben utilizar encriptación si persisten datos sensibles en disco.

---

## 4. Fase 4 — Calidad de Sintaxis Kotlin
- **Objetivo:** Prevenir fallos silenciosos de nulabilidad.
- **Verificación:**
  - Buscar ocurrencias del operador doble bang `!!`:
    - ❌ Prohibido el uso de `!!`.
    - ✅ Obligatorio usar Elvis operator (`?:`), safe-calls (`?.`) o `requireNotNull()` con mensaje descriptivo.

---

## 5. Fase 5 — Ejecución de Pruebas y Análisis Estático
- **Ejecutar Pruebas Unitarias:**
  ```powershell
  ./gradlew testDebugUnitTest
  ```
- **Ejecutar Análisis Estático (Lint / Detekt):**
  ```powershell
  ./gradlew lintDebug
  ```
- **Criterio de Aprobación Final:** Todas las pruebas pasan en verde (BUILD SUCCESSFUL) con 0 errores fatales de lint.
