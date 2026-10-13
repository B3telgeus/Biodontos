---
name: quality-gate
description: Pipeline automatizado de auditoría de arquitectura, análisis estático, pureza de dominio, manejo de errores y privacidad HIPAA.
---

# Workflow /quality-gate: Auditoría y Verificación de Calidad

Ejecuta el pipeline completo de auditoría técnica y cumplimiento antes de integrar o realizar commit de código.

Para la guía detallada de orquestación, consulta [.agents/workflows/quality-gate.md](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/.agents/workflows/quality-gate.md).

---

## Fases de Verificación

1. **Pureza de Dominio:** Ejecutar `.agents/skills/quality-detekt-verify/scripts/verify-domain-purity.ps1` (0 dependencias de Android en `domain`).
2. **Manejo Funcional de Errores:** Validar que no existan `throw Exception` en lógica de negocio (uso estricto de `Either` o `Result`).
3. **Privacidad HIPAA:** Comprobar que no existan datos de pacientes (PHI) en logs ni trazas de consola.
4. **Calidad Kotlin:** Prohibido el uso de `!!`.
5. **Pruebas y Build:** Ejecutar `./gradlew testDebugUnitTest` y `./gradlew lintDebug`.
