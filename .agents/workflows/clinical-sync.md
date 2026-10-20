# WORKFLOW: /clinical-sync (Integración Segura Open Dental & Offline-First)

Este flujo de trabajo orquesta la integración transaccional y la sincronización offline-first de citas, fichas de pacientes y odontogramas con la API REST de Open Dental y Supabase.

---

## 1. Contrato de API y Endpoints Especializados
- **Verificación de Métodos HTTP:**
  - En citas médicas (`/appointments`), **prohibido usar PUT genérico** que pueda sobrescribir procedimientos asignados.
  - Utilizar exclusivamente endpoints especializados transaccionales:
    - Confirmar cita: `PUT /appointments/{aptNum}/Confirm`
    - Agregar o editar nota: `PUT /appointments/{aptNum}/Note`
    - Mover a tablero: `PUT /appointments/{aptNum}/Pinboard`
- **Paginación y Fechas:**
  - Incluir obligatoriamente parámetros `Limit` y `Offset` en llamadas masivas (`/patients`, `/allergies`).
  - Parsear fechas de Open Dental (`yyyy-MM-dd HH:mm:ss`) a `java.time.LocalDateTime` en la capa de datos.

---

## 2. Estrategia Offline-First con Room
Para garantizar que el odontólogo pueda operar en el gabinete dental ante caídas de red:
1. **Cache-First Pattern:**
   - La UI consulta la base de datos local de Room mediante `Flow<List<Entity>>`.
   - La red actualiza la base de datos local, disparando la reactividad en Compose.
2. **Transacciones Clínicas:**
   - Toda actualización que involucre múltiples superficies o piezas dentales simultáneas debe ejecutarse dentro de un bloque `@Transaction`.
3. **Control de Estados de Sincronización:**
   - Cada entidad persistida debe incluir una bandera de sincronización:
     - `SYNCED`: Confirmado por el servidor.
     - `PENDING_UPLOAD`: Modificado offline, en cola de sincronización.
     - `CONFLICT`: Requiere intervención o resolución por marca temporal.

---

## 3. Cola de Sincronización en Segundo Plano (WorkManager)
1. **Implementar `CoroutineWorker`:**
   - Registrar la tarea en `androidx.work.WorkManager`.
2. **Restricciones de Red:**
   - Configurar restricciones con `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()`.
3. **Reintentos Exponenciales:**
   - Configurar `BackoffPolicy.EXPONENTIAL` ante errores transitorios de red (429, 503, Timeout).

---

## 4. Checklist de Validación del Workflow
- [ ] No se utilizaron llamadas destructivas (`PUT` genérico) en citas.
- [ ] Las entidades de Room cuentan con estado de sincronización.
- [ ] El mapper `toDomain()` maneja campos nulos defensivamente.
- [ ] La sincronización en segundo plano es idempotente (no duplica registros al reintentar).
