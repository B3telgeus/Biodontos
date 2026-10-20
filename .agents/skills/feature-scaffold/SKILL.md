---
name: feature-scaffold
description: Pipeline de generación de funcionalidades clínicas odontológicas de extremo a extremo (Dominio, Persistencia, Compose UI y Tests).
---

# Workflow /feature-scaffold: Generación E2E Odontológica

Ejecuta el pipeline completo de desarrollo para una nueva característica clínica en **Biodontos**.

Para la guía detallada de orquestación, consulta [.agents/workflows/feature-scaffold.md](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/.agents/workflows/feature-scaffold.md).

---

## Pasos de Ejecución Automática

1. **Dominio:** Activar `dental-usecase-generator` para definir entidades, Value Classes (`ToothFdi`, `PatientId`) y caso de uso con `Either`.
2. **Persistencia / DTOs:** Activar `dental-patient-privacy-mapper` para definir DTOs con `@SerialName("PascalCase")` y mappers seguros sin PHI.
3. **Inyección:** Registrar instancias en [AppContainer.kt](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/app/src/main/java/com/example/biodontos/core/di/AppContainer.kt).
4. **UI Compose:** Activar `dental-odontogram-compose` para generar Composable Stateful (`*Route`) y Stateless (`*Screen` con `@VisibleForTesting`), integrando en `Screen.kt` y `NavGraph.kt`. Garantizar consciencia de insets mediante `BiodontosScreen`, `BiodontosBottomActionBar` y `BiodontosFormColumn`.
5. **Testing:** Generar pruebas unitarias del ViewModel y UseCase con MockK y Turbine.
