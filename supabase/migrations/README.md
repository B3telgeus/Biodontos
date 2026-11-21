# Biodontos — Supabase Database Migrations

Este directorio contiene las migraciones SQL para el backend PostgreSQL / Supabase de la plataforma odontológica **Biodontos**, diseñadas con cumplimiento de estándares de seguridad médica, integridad transaccional ACID y Row Level Security (RLS).

---

## 1. Convención de Nomenclatura y Orden Cronológico

Las migraciones siguen el estándar de 14 dígitos de Supabase CLI (`YYYYMMDDHHMMSS_nombre.sql`), ordenadas estrictamente de acuerdo con el grafo de dependencias de entidades:

```
supabase/migrations/
├── 20261001000000_base_schema.sql
├── 20261013_patient_portal.sql
├── 20261020000000_materials_and_atomic_evolutions.sql
├── 20261121000000_add_clinical_consent_form_columns.sql
└── README.md
```

### Tabla de Ejecución de Migraciones

| Orden | Archivo | Descripción Principal | Dependencias |
|---|---|---|---|
| **1** | `20261001000000_base_schema.sql` | Esquema base con 15 tablas clínicas, helper `is_doctor()`, trigger en `auth.users` (`handle_new_user()`), índices y RLS. | Extensión `pgcrypto` y esquema `auth` de Supabase. |
| **2** | `20261013_patient_portal.sql` | Portal del paciente: columnas de perfil doctor, tabla `doctor_availability`, índice anti-colisión de citas, RPCs de agendamiento y RLS. | Tablas `doctors`, `patients`, `appointments`, función `is_doctor()`. |
| **3** | `20261020000000_materials_and_atomic_evolutions.sql` | Control INVIMA (`clinical_materials`), consumo de materiales (`evolution_materials_used`), bucket `doctor-avatars` y RPC transaccional `save_evolution_with_materials`. | Tablas `doctors`, `clinical_records`, `evolutions`, función `is_doctor()`. |
| **4** | `20261121000000_add_clinical_consent_form_columns.sql` | Extensión de columnas e índices para consentimientos clínicos especializados (Blanqueamiento, Odontología General, Rehabilitación Oral). | Tablas `informed_consents`, `informed_consent_procedures`. |

---

## 2. Detalle de Migraciones

### 1. `20261001000000_base_schema.sql` (Esquema Base)
- **Propósito**: Establecer el núcleo relacional del sistema clínico y la sincronización automática de perfiles.
- **Entidades Creadas (15 tablas)**:
  1. `public.doctors`: Directorio profesional y datos de especialidad/licencia.
  2. `public.guardians`: Registro de acudientes para pacientes menores de edad.
  3. `public.patients`: Ficha demográfica y clínica del paciente (DNI único, EPS, alergias críticas, etc.).
  4. `public.appointments`: Agenda de citas médicas y control de estados.
  5. `public.clinical_records`: Contenedor principal de la historia clínica (diagnósticos y estado de cierre).
  6. `public.anamnesis`: Formulario de 20 antecedentes sistémicos patológicos/no patológicos.
  7. `public.evolutions`: Registro de notas de evolución clínica, cobro, balance y firmas.
  8. `public.tooth_states`: Mapeo de superficies dentales (nomenclatura FDI) ligadas a evoluciones.
  9. `public.periodontal_examinations`: Cabecera de sesiones de examen periodontograma.
  10. `public.periodontal_sites`: Mediciones por sitio (6 sitios por diente, bolsas, recesión, movilidad).
  11. `public.informed_consents`: Consentimientos informados clínicos y quirúrgicos con firmas.
  12. `public.informed_consent_procedures`: Procedimientos específicos autorizados en el consentimiento.
  13. `public.clinical_audit_log`: Registro inmutable de auditoría para trazabilidad médica legal.
  14. `public.treatments`: Planes de tratamiento odontológico y presupuestos.
  15. `public.payments`: Comprobantes de pago y abonos por tratamiento.
- **Seguridad y Funciones**:
  - `public.is_doctor()`: Retorna booleano evaluando si `auth.uid()` corresponde a un doctor activo en `public.doctors` (`SECURITY DEFINER SET search_path = public, pg_temp`).
  - `public.handle_new_user()` + trigger `on_auth_user_created AFTER INSERT ON auth.users`: Extrae metadatos de `NEW.raw_user_meta_data` (`role`, `first_name`, `last_name`, `dni_cedula`, `license_number`, etc.) e inserta atómicamente el perfil en `public.patients` o `public.doctors`. Previene errores si el usuario requiere verificación previa por email.

---

### 2. `20261013_patient_portal.sql` (Portal de Pacientes y Agendamiento)
- **Propósito**: Gestión de agenda de doctores, reserva de citas desde el portal del paciente y prevención de solapamientos.
- **Entidades Creadas/Modificadas**:
  - `ALTER TABLE public.doctors`: Incorporación de `bio` y `photo_url`.
  - `public.doctor_availability`: Horarios semanales configurables por cada profesional.
  - Índice único `ux_appointments_doctor_slot`: Garantiza a nivel de base de datos que ningún doctor tenga dos citas activas en el mismo intervalo de inicio (`WHERE status <> 'CANCELLED'`).
- **Endurecimiento de Seguridad (Security Hardening)**:
  - Todas las 4 funciones RPC (`get_doctor_busy_slots`, `patient_book_appointment`, `patient_reschedule_appointment`, `patient_cancel_appointment`) han sido blindadas con:
    1. `SECURITY DEFINER SET search_path = public, pg_temp` (previene ataques de inyección por sustitución de search_path).
    2. Validación obligatoria de sesión autenticada con `auth.uid() IS NULL`.
    3. Validación de propiedad mediante `WHERE user_id = auth.uid()` en lugar de parseo inseguro de JWT.

---

### 3. `20261020000000_materials_and_atomic_evolutions.sql` (Materiales y Atomicidad)
- **Propósito**: Trazabilidad de lotes e INVIMA en materiales clínicos y atomicidad estricta en el guardado de evoluciones.
- **Entidades Creadas**:
  - `public.clinical_materials`: Stock de materiales odontológicos con restricciones:
    - `CONSTRAINT ux_clinical_materials UNIQUE (invima_registration, batch_lot_number)`
    - `CONSTRAINT chk_clinical_materials CHECK (expiration_date >= manufacturing_date)`
  - `public.evolution_materials_used`: Materiales consumidos vinculados a la evolución (`ON DELETE CASCADE` hacia evolución, `ON DELETE RESTRICT` hacia material para proteger histórico).
- **Almacenamiento (Supabase Storage)**:
  - Bucket `doctor-avatars`: Público para visualización (`public = true`), límite de 5 MB, tipos MIME permitidos: `image/jpeg`, `image/png`, `image/webp`.
  - Políticas RLS sobre `storage.objects`:
    - Lectura pública para cualquier usuario (`bucket_id = 'doctor-avatars'`).
    - Subida, edición y borrado restringidos exclusivamente al doctor dueño de la carpeta mediante validación de prefijo `(storage.foldername(name))[1] IN (SELECT id::text FROM public.doctors WHERE user_id = auth.uid())`.
- **RPC Transaccional `save_evolution_with_materials`**:
  - `SECURITY DEFINER SET search_path = public, pg_temp`.
  - Valida rol de doctor autenticado (`public.is_doctor()`).
  - Ejecuta en una única transacción atómica:
    1. Upsert en `public.evolutions`.
    2. Borrado de materiales previos de dicha evolución (`DELETE FROM evolution_materials_used WHERE evolution_id = ...`) seguido de la inserción de nuevos materiales (`p_materials`), asegurando total **idempotencia** ante reintentos por caída de red.
    3. Si `p_close_record = true`, actualiza la historia clínica (`public.clinical_records`) a estado `'CERRADA'`, estampando `closed_at = now()`, `closed_by_doctor_id`, `closure_summary` y `closure_signature_url`.
    4. Retorna objeto JSON `{ "success": true, "evolution_id": "<uuid>" }`.

---

### 4. `20261121000000_add_clinical_consent_form_columns.sql` (Formularios Clínicos de Consentimiento)
- **Propósito**: Extender el esquema para soportar los tres nuevos formularios de consentimiento informado clínico (Odontología General, Blanqueamiento Dental y Rehabilitación Oral) manteniendo plena compatibilidad con registros existentes.
- **Modificaciones en `public.informed_consents`**:
  - `consent_type text NOT NULL DEFAULT 'GENERAL_ADMISSION'`: Clasificación con restricción CHECK (`chk_informed_consents_consent_type`).
  - `patient_city text DEFAULT 'Bogotá D.C.'`: Lugar de suscripción legal del consentimiento.
  - `selected_procedures jsonb NOT NULL DEFAULT '[]'::jsonb`: Lista JSON de procedimientos autorizados.
  - `acknowledged_risks jsonb NOT NULL DEFAULT '[]'::jsonb`: Lista JSON de riesgos informados y aceptados.
  - `relevant_medical_conditions text`: Antecedentes y condiciones clínicas relevantes.
  - `occlusion_test_accepted boolean NOT NULL DEFAULT false`: Aceptación de prueba oclusal.
  - `aesthetic_color_accepted boolean NOT NULL DEFAULT false`: Aceptación estética de color y anatomía.
  - `initial_color_guide text`: Guía inicial de color (VITA/estándar).
  - `final_color_guide text`: Guía final de color alcanzada.
  - `specific_notes text`: Notas y acuerdos clínicos específicos.
- **Modificaciones en `public.informed_consent_procedures`**:
  - `procedure_name text`: Denominación del procedimiento dental específico.
  - `doctor_signature_url text`: Firma del profesional responsable del procedimiento individual.
- **Índices**:
  - `idx_informed_consents_patient_type` sobre `(patient_id, consent_type)` para consultas por paciente y especialidad.
  - `idx_informed_consents_type` sobre `(consent_type)` para filtrado por tipo de formulario.

---

## 3. Guía de Ejecución

### Aplicación con Supabase CLI
```bash
# Vincular con el proyecto de Supabase (si no está vinculado)
supabase link --project-ref <PROJECT_REF>

# Aplicar todas las migraciones en orden cronológico
supabase db push
```

### Aplicación Manual en PostgreSQL / SQL Editor
Si se aplican los scripts a través del panel de Supabase SQL Editor o cliente `psql`:
1. Ejecutar `20261001000000_base_schema.sql`
2. Ejecutar `20261013_patient_portal.sql`
3. Ejecutar `20261020000000_materials_and_atomic_evolutions.sql`
4. Ejecutar `20261121000000_add_clinical_consent_form_columns.sql`
