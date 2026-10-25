-- =============================================================================
-- MIGRACIÓN: MÓDULO CRUD DE HISTORIA CLÍNICA Y TRAZABILIDAD DEL PACIENTE
-- Cumplimiento: Resolución 1995 de 1999 (Inmutabilidad y Trazabilidad Clínica)
-- Ejecutar en: Supabase SQL Editor
-- =============================================================================

-- 1. Extender el ENUM public.treatment_status (si la columna status usa el tipo enum)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'treatment_status') THEN
    ALTER TYPE public.treatment_status ADD VALUE IF NOT EXISTS 'PENDIENTE';
    ALTER TYPE public.treatment_status ADD VALUE IF NOT EXISTS 'APROBADO';
    ALTER TYPE public.treatment_status ADD VALUE IF NOT EXISTS 'EN_CURSO';
    ALTER TYPE public.treatment_status ADD VALUE IF NOT EXISTS 'COMPLETADO';
    ALTER TYPE public.treatment_status ADD VALUE IF NOT EXISTS 'CANCELADO';
  END IF;
END $$;

-- 2. Evoluciones: campos de anulación lógica y nota aclaratoria vinculada
ALTER TABLE public.evolutions
  ADD COLUMN IF NOT EXISTS voided_at timestamptz,
  ADD COLUMN IF NOT EXISTS voided_by uuid REFERENCES auth.users(id),
  ADD COLUMN IF NOT EXISTS void_reason text CHECK (void_reason IS NULL OR length(trim(void_reason)) >= 10),
  ADD COLUMN IF NOT EXISTS amends_evolution_id uuid REFERENCES public.evolutions(id);

-- 3. Pagos: campos de anulación lógica contable
ALTER TABLE public.payments
  ADD COLUMN IF NOT EXISTS voided_at timestamptz,
  ADD COLUMN IF NOT EXISTS voided_by uuid REFERENCES auth.users(id),
  ADD COLUMN IF NOT EXISTS void_reason text CHECK (void_reason IS NULL OR length(trim(void_reason)) >= 10);

-- 4. Tratamientos/Presupuestos: motivo de cambio de estado / cancelación
ALTER TABLE public.treatments
  ADD COLUMN IF NOT EXISTS status_reason text;

-- 5. Tabla de Auditoría de Actividad Clínica
CREATE TABLE IF NOT EXISTS public.clinical_audit_log (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id uuid NOT NULL REFERENCES public.patients(id) ON DELETE CASCADE,
  entity_table text NOT NULL CHECK (
    entity_table IN (
      'appointments',
      'payments',
      'evolutions',
      'treatments',
      'informed_consents',
      'clinical_records'
    )
  ),
  entity_id uuid NOT NULL,
  action text NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'VOID', 'STATUS_CHANGE')),
  changed_fields text[] DEFAULT '{}',
  actor_user_id uuid REFERENCES auth.users(id),
  actor_name text,
  occurred_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_patient_time
  ON public.clinical_audit_log(patient_id, occurred_at DESC);

ALTER TABLE public.clinical_audit_log ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores leen auditoria" ON public.clinical_audit_log;
CREATE POLICY "Doctores leen auditoria" ON public.clinical_audit_log
  FOR SELECT TO authenticated USING (public.is_doctor());

-- 6. Trigger SECURITY DEFINER para registro automático de auditoría clínica
CREATE OR REPLACE FUNCTION public.log_clinical_activity()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_new jsonb := to_jsonb(NEW);
  v_old jsonb := CASE WHEN TG_OP = 'UPDATE' THEN to_jsonb(OLD) ELSE '{}'::jsonb END;
  v_patient_id uuid;
  v_entity_id uuid;
  v_action text;
  v_actor_name text;
  v_changed text[] := '{}';
BEGIN
  -- Resolver nombre del profesional desde la tabla doctors (first_name + last_name) o JWT
  SELECT NULLIF(TRIM(CONCAT(first_name, ' ', last_name)), '')
  INTO v_actor_name
  FROM public.doctors
  WHERE user_id = auth.uid() OR id = auth.uid()
  LIMIT 1;

  IF v_actor_name IS NULL THEN
    v_actor_name := coalesce(auth.jwt() ->> 'email', 'Sistema Clínico');
  END IF;

  v_entity_id := (v_new ->> 'id')::uuid;

  -- Resolver patient_id según la tabla origen
  CASE TG_TABLE_NAME
    WHEN 'appointments', 'treatments', 'informed_consents', 'clinical_records' THEN
      v_patient_id := NULLIF(v_new ->> 'patient_id', '')::uuid;
    WHEN 'payments' THEN
      SELECT t.patient_id INTO v_patient_id
      FROM public.treatments t
      WHERE t.id = NULLIF(v_new ->> 'treatment_id', '')::uuid;
    WHEN 'evolutions' THEN
      SELECT cr.patient_id INTO v_patient_id
      FROM public.clinical_records cr
      WHERE cr.id = NULLIF(v_new ->> 'record_id', '')::uuid;
  END CASE;

  IF v_patient_id IS NULL OR v_entity_id IS NULL THEN
    RETURN NEW;
  END IF;

  -- Determinar tipo de acción y campos modificados usando JSONB seguro
  IF TG_OP = 'INSERT' THEN
    v_action := 'CREATE';
  ELSIF TG_OP = 'UPDATE' THEN
    IF TG_TABLE_NAME IN ('evolutions', 'payments')
       AND (v_old ->> 'voided_at') IS NULL
       AND (v_new ->> 'voided_at') IS NOT NULL THEN
      v_action := 'VOID';
      v_changed := ARRAY['voided_at', 'void_reason'];
    ELSIF (v_old ->> 'status') IS DISTINCT FROM (v_new ->> 'status') THEN
      v_action := 'STATUS_CHANGE';
      v_changed := array_append(v_changed, 'status');
    ELSIF TG_TABLE_NAME = 'appointments'
          AND (v_old ->> 'start_time') IS DISTINCT FROM (v_new ->> 'start_time') THEN
      v_action := 'UPDATE';
      v_changed := array_append(v_changed, 'start_time');
    ELSE
      v_action := 'UPDATE';
    END IF;
  END IF;

  INSERT INTO public.clinical_audit_log (
    patient_id,
    entity_table,
    entity_id,
    action,
    changed_fields,
    actor_user_id,
    actor_name,
    occurred_at
  ) VALUES (
    v_patient_id,
    TG_TABLE_NAME,
    v_entity_id,
    v_action,
    v_changed,
    auth.uid(),
    v_actor_name,
    now()
  );

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_audit_appointments ON public.appointments;
CREATE TRIGGER trg_audit_appointments
  AFTER INSERT OR UPDATE ON public.appointments
  FOR EACH ROW EXECUTE FUNCTION public.log_clinical_activity();

DROP TRIGGER IF EXISTS trg_audit_payments ON public.payments;
CREATE TRIGGER trg_audit_payments
  AFTER INSERT OR UPDATE ON public.payments
  FOR EACH ROW EXECUTE FUNCTION public.log_clinical_activity();

DROP TRIGGER IF EXISTS trg_audit_evolutions ON public.evolutions;
CREATE TRIGGER trg_audit_evolutions
  AFTER INSERT OR UPDATE ON public.evolutions
  FOR EACH ROW EXECUTE FUNCTION public.log_clinical_activity();

DROP TRIGGER IF EXISTS trg_audit_treatments ON public.treatments;
CREATE TRIGGER trg_audit_treatments
  AFTER INSERT OR UPDATE ON public.treatments
  FOR EACH ROW EXECUTE FUNCTION public.log_clinical_activity();

DROP TRIGGER IF EXISTS trg_audit_consents ON public.informed_consents;
CREATE TRIGGER trg_audit_consents
  AFTER INSERT OR UPDATE ON public.informed_consents
  FOR EACH ROW EXECUTE FUNCTION public.log_clinical_activity();

DROP TRIGGER IF EXISTS trg_audit_clinical_records ON public.clinical_records;
CREATE TRIGGER trg_audit_clinical_records
  AFTER INSERT OR UPDATE ON public.clinical_records
  FOR EACH ROW EXECUTE FUNCTION public.log_clinical_activity();

-- 7. Trigger BEFORE UPDATE OR DELETE para garantizar inmutabilidad de Evoluciones y Pagos
CREATE OR REPLACE FUNCTION public.prevent_clinical_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_old_core jsonb;
  v_new_core jsonb;
BEGIN
  IF TG_OP = 'DELETE' THEN
    RAISE EXCEPTION 'Operación prohibida por normativa clínica (Res. 1995/1999): No se permite eliminar registros de % físicamente. Utilice la anulación lógica.', TG_TABLE_NAME;
  END IF;

  IF TG_OP = 'UPDATE' THEN
    IF OLD.voided_at IS NOT NULL THEN
      RAISE EXCEPTION 'Operación prohibida: El registro en % ya fue anulado previamente y es inmutable.', TG_TABLE_NAME;
    END IF;

    IF NEW.voided_at IS NULL OR NEW.void_reason IS NULL OR length(trim(NEW.void_reason)) < 10 THEN
      RAISE EXCEPTION 'Operación prohibida: Solo se permite actualizar % para registrar una anulación lógica con motivo válido (>= 10 caracteres).', TG_TABLE_NAME;
    END IF;

    -- Comparar todas las demás columnas para garantizar que ningún dato clínico o contable cambió
    v_old_core := to_jsonb(OLD) - 'voided_at' - 'voided_by' - 'void_reason' - 'updated_at';
    v_new_core := to_jsonb(NEW) - 'voided_at' - 'voided_by' - 'void_reason' - 'updated_at';

    IF v_old_core IS DISTINCT FROM v_new_core THEN
      RAISE EXCEPTION 'Operación prohibida: Los datos clínicos o contables de % no pueden ser alterados. Registre una nota aclaratoria o anule el registro.', TG_TABLE_NAME;
    END IF;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_immutable_evolutions ON public.evolutions;
CREATE TRIGGER trg_immutable_evolutions
  BEFORE UPDATE OR DELETE ON public.evolutions
  FOR EACH ROW EXECUTE FUNCTION public.prevent_clinical_mutation();

DROP TRIGGER IF EXISTS trg_immutable_payments ON public.payments;
CREATE TRIGGER trg_immutable_payments
  BEFORE UPDATE OR DELETE ON public.payments
  FOR EACH ROW EXECUTE FUNCTION public.prevent_clinical_mutation();

-- 8. Ajuste de políticas RLS en payments y treatments (reemplazar FOR ALL que permitía DELETE físico)
DROP POLICY IF EXISTS "Doctores gestionan pagos" ON public.payments;
DROP POLICY IF EXISTS "Doctores leen pagos" ON public.payments;
DROP POLICY IF EXISTS "Doctores insertan pagos" ON public.payments;
DROP POLICY IF EXISTS "Doctores anulan pagos" ON public.payments;

CREATE POLICY "Doctores leen pagos" ON public.payments
  FOR SELECT TO authenticated USING (public.is_doctor());

CREATE POLICY "Doctores insertan pagos" ON public.payments
  FOR INSERT TO authenticated WITH CHECK (public.is_doctor());

CREATE POLICY "Doctores anulan pagos" ON public.payments
  FOR UPDATE TO authenticated
  USING (public.is_doctor() AND voided_at IS NULL)
  WITH CHECK (public.is_doctor() AND voided_at IS NOT NULL);

DROP POLICY IF EXISTS "Doctores gestionan tratamientos" ON public.treatments;
DROP POLICY IF EXISTS "Doctores leen tratamientos" ON public.treatments;
DROP POLICY IF EXISTS "Doctores insertan tratamientos" ON public.treatments;
DROP POLICY IF EXISTS "Doctores actualizan tratamientos" ON public.treatments;

CREATE POLICY "Doctores leen tratamientos" ON public.treatments
  FOR SELECT TO authenticated USING (public.is_doctor());

CREATE POLICY "Doctores insertan tratamientos" ON public.treatments
  FOR INSERT TO authenticated WITH CHECK (public.is_doctor());

CREATE POLICY "Doctores actualizan tratamientos" ON public.treatments
  FOR UPDATE TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

-- 9. Notificar a PostgREST para recargar el esquema
NOTIFY pgrst, 'reload schema';
