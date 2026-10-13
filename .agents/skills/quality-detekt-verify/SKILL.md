---
name: quality-detekt-verify
description: Workflow de análisis estático y verificación de arquitectura de Biodontos. Valida pureza del módulo de dominio (cero Android imports) y ejecuta tareas de verificación y calidad de código.
---

# Workflow de Calidad, Análisis Estático y Pureza Arquitectónica

Esta Skill proporciona el procedimiento automatizado para auditar y verificar la calidad técnica, el análisis estático y el cumplimiento arquitectónico en **Biodontos**.

---

## 1. Reglas de Calidad Obligatorias

1. **Pureza Absoluta de Dominio:** Ninguna clase dentro de `com.example.biodontos.domain` puede importar paquetes de `android.*` o `androidx.*`.
2. **Cero `!!` (Doble Bang):** Ningún archivo de producción o test debe contener operadores de aserción forzada `!!`.
3. **Cero PHI en Logs:** Prohibido registrar identificadores de pacientes en logs o salidas por consola.

---

## 2. Procedimiento de Ejecución Paso a Paso

### Paso 1: Verificar Pureza del Dominio (Script PowerShell)
Ejecuta el script incluido en los recursos auxiliares de la skill:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/skills/quality-detekt-verify/scripts/verify-domain-purity.ps1
```
Si detecta alguna importación de Android en `domain`, la refactorización debe corregirla inmediatamente reemplazándola por abstracciones puras de Kotlin.

### Paso 2: Análisis Estático con Lint / Detekt
Ejecutar el análisis estático de Android Gradle:
```powershell
./gradlew lintDebug
```
Verificar que no se introduzcan advertencias críticas o de accesibilidad.

### Paso 3: Ejecución de Tests Unitarios de Regresión
Asegurar que todas las pruebas pasen sin fallos:
```powershell
./gradlew testDebugUnitTest
```

---

## 3. Lista de Verificación Previa a Commit / PR

- [ ] Script de pureza de dominio ejecutado y en verde (0 violaciones).
- [ ] No existen usos de `!!` en los archivos modificados.
- [ ] Todos los nuevos Casos de Uso o ViewModels cuentan con tests unitarios asociados.
- [ ] Las suites de pruebas unitarias (`testDebugUnitTest`) finalizan con éxito.
