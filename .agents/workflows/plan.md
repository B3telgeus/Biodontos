# WORKFLOW: /plan (Ciclo Plan-Execute-Verify)

Este flujo de trabajo coordina la planificación, ejecución y verificación controlada para tareas complejas o multifichero en **Biodontos**.

---

## 1. Fase de Análisis e Ingesta de Contexto
Antes de modificar o generar código:
1. **Consultar las Directrices del Proyecto:**
   - Leer [AGENTS.md](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/AGENTS.md) para conocer el estado actual y stack técnico.
   - Leer [GEMINI.md](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/GEMINI.md) para validar reglas clínicas y restricciones de API.
2. **Identificar Módulos Impactados:**
   - Evaluar si la tarea afecta a `:domain`, `:data`, `:presentation` o `:core`.

---

## 2. Fase de Elaboración del Plan (`/plan`)
El Orquestador debe generar un artefacto de planificación que contenga:
- **Resumen del Objetivo Clínico/Técnico.**
- **Checklist Atómico de Tareas por Capas:**
  - `[ ] Capa Domain`: Entidades, Value Classes, UseCase con `Either`.
  - `[ ] Capa Data`: DTOs (`@SerialName`), Mappers (`toDomain`), Repositorio / Room.
  - `[ ] Capa Core / DI`: Registro perezoso en `AppContainer`.
  - `[ ] Capa Presentation`: ViewModel (`StateFlow`), NavGraph, Composables (Stateful / Stateless).
  - `[ ] Capa Testing`: Pruebas unitarias con MockK y Turbine.
- **Criterios de Aceptación Médicos y Técnicos:**
  - Validación FDI (11..48, 51..85).
  - Cero PHI en logs.
  - Validación de Consentimiento Informado.

---

## 3. Fase de Ejecución Secuencial
Modificar los archivos respetando el flujo de dependencias de Clean Architecture:
1. **Paso 1 (Dominio Primero):** Implementar modelos y casos de uso puros.
2. **Paso 2 (Infraestructura / Datos):** Implementar DTOs, mappers y adaptadores de repositorio.
3. **Paso 3 (Inyección de Dependencias):** Conectar las nuevas instancias en `AppContainer`.
4. **Paso 4 (Presentación / UI):** Crear ViewModel y Composables.
5. **Paso 5 (Navegación):** Registrar ruta tipada en `Screen.kt` y `NavGraph.kt`.

---

## 4. Fase de Verificación y Cierre
Validar automáticamente la solución ejecutando los comandos de calidad:
1. Ejecutar test de pureza arquitectónica:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/skills/quality-detekt-verify/scripts/verify-domain-purity.ps1
   ```
2. Ejecutar suite de pruebas unitarias:
   ```powershell
   ./gradlew testDebugUnitTest
   ```
3. Verificar compilación completa:
   ```powershell
   ./gradlew assembleDebug
   ```
