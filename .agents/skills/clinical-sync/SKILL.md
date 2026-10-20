---
name: clinical-sync
description: Workflow de integración segura con la API de Open Dental y sincronización offline-first con Room y WorkManager.
---

# Workflow /clinical-sync: Integración Open Dental & Offline-First

Orquesta la integración de citas, fichas y odontogramas con Open Dental API y sincronización transaccional local.

Para la guía detallada de orquestación, consulta [.agents/workflows/clinical-sync.md](file:///c:/Users/DELL/AndroidStudioProjects/Biodontos/.agents/workflows/clinical-sync.md).

---

## Directrices de Ejecución

1. **Endpoints Especializados:** Prohibido `PUT` genérico en citas (`/appointments/{id}`). Usar `/Confirm` y `/Note`.
2. **Paginación & Fechas:** Parámetros `Limit` y `Offset` obligatorios. Fechas parseadas a `LocalDateTime`.
3. **Persistencia Room:** Almacenar con estados de sincronización (`SYNCED`, `PENDING_UPLOAD`, `CONFLICT`) bajo `@Transaction`.
4. **Sincronización:** Tareas en segundo plano con WorkManager (`CoroutineWorker`) y reintentos exponenciales.
