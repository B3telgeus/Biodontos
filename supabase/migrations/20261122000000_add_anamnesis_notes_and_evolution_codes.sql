-- ============================================================================
-- Biodontos - Migración: Notas por opción en Anamnesis y Códigos CIE-10 / CUPS en Evoluciones
-- Fecha: 2026-11-22
-- ============================================================================

-- 1. EXTENDER TABLA: public.anamnesis
ALTER TABLE public.anamnesis
  ADD COLUMN IF NOT EXISTS option_notes jsonb NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN public.anamnesis.option_notes IS 'Mapa JSON (clave de pregunta -> nota clínica) con detalles para las opciones activadas en la anamnesis';

-- 2. EXTENDER TABLA: public.evolutions
ALTER TABLE public.evolutions
  ADD COLUMN IF NOT EXISTS cie10_code text,
  ADD COLUMN IF NOT EXISTS cie10_description text,
  ADD COLUMN IF NOT EXISTS cups_code text,
  ADD COLUMN IF NOT EXISTS cups_description text;

COMMENT ON COLUMN public.evolutions.cie10_code IS 'Código de diagnóstico/tratamiento CIE-10 asociado a la evolución';
COMMENT ON COLUMN public.evolutions.cie10_description IS 'Descripción del código CIE-10 seleccionado';
COMMENT ON COLUMN public.evolutions.cups_code IS 'Código CUPS del procedimiento odontológico realizado';
COMMENT ON COLUMN public.evolutions.cups_description IS 'Descripción del procedimiento CUPS realizado';

CREATE INDEX IF NOT EXISTS idx_evolutions_cie10_code ON public.evolutions (cie10_code);
CREATE INDEX IF NOT EXISTS idx_evolutions_cups_code ON public.evolutions (cups_code);

-- 3. ACTUALIZAR RPC TRANSACCIONAL: public.save_evolution_with_materials
CREATE OR REPLACE FUNCTION public.save_evolution_with_materials(
  p_evolution jsonb,
  p_materials jsonb,
  p_close_record boolean DEFAULT false,
  p_closure_summary text DEFAULT NULL,
  p_closure_signature_url text DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_evolution_id uuid;
  v_record_id uuid;
  v_doctor_id uuid;
  v_mat jsonb;
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated' USING ERRCODE = '28000';
  END IF;

  IF NOT public.is_doctor() THEN
    RAISE EXCEPTION 'Forbidden: only doctors can save evolutions' USING ERRCODE = '42501';
  END IF;

  SELECT id INTO v_doctor_id
  FROM public.doctors
  WHERE user_id = auth.uid()
  LIMIT 1;

  IF v_doctor_id IS NULL THEN
    RAISE EXCEPTION 'Doctor profile not found for authenticated user' USING ERRCODE = 'P0002';
  END IF;

  v_evolution_id := COALESCE(NULLIF(p_evolution ->> 'id', '')::uuid, gen_random_uuid());
  v_record_id := (p_evolution ->> 'record_id')::uuid;

  IF v_record_id IS NULL THEN
    RAISE EXCEPTION 'record_id is required in evolution payload' USING ERRCODE = '23502';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.clinical_records WHERE id = v_record_id) THEN
    RAISE EXCEPTION 'Clinical record with id % not found', v_record_id USING ERRCODE = 'P0002';
  END IF;

  INSERT INTO public.evolutions (
    id,
    record_id,
    doctor_id,
    date,
    procedure_time,
    description,
    doctor_name,
    tooth_treated,
    cost,
    payment_amount,
    balance,
    patient_signature_url,
    doctor_signature_url,
    voided_at,
    voided_by,
    void_reason,
    amends_evolution_id,
    cie10_code,
    cie10_description,
    cups_code,
    cups_description,
    created_at,
    updated_at
  ) VALUES (
    v_evolution_id,
    v_record_id,
    v_doctor_id,
    COALESCE(NULLIF(p_evolution ->> 'date', '')::date, CURRENT_DATE),
    COALESCE(NULLIF(p_evolution ->> 'procedure_time', '')::time, CURRENT_TIME),
    COALESCE(p_evolution ->> 'description', ''),
    p_evolution ->> 'doctor_name',
    NULLIF(p_evolution ->> 'tooth_treated', '')::integer,
    COALESCE(NULLIF(p_evolution ->> 'cost', '')::numeric, 0.0),
    COALESCE(NULLIF(p_evolution ->> 'payment_amount', '')::numeric, 0.0),
    COALESCE(NULLIF(p_evolution ->> 'balance', '')::numeric, 0.0),
    p_evolution ->> 'patient_signature_url',
    p_evolution ->> 'doctor_signature_url',
    NULLIF(p_evolution ->> 'voided_at', '')::timestamptz,
    p_evolution ->> 'voided_by',
    p_evolution ->> 'void_reason',
    NULLIF(p_evolution ->> 'amends_evolution_id', '')::uuid,
    NULLIF(p_evolution ->> 'cie10_code', ''),
    NULLIF(p_evolution ->> 'cie10_description', ''),
    NULLIF(p_evolution ->> 'cups_code', ''),
    NULLIF(p_evolution ->> 'cups_description', ''),
    now(),
    now()
  )
  ON CONFLICT (id) DO UPDATE SET
    record_id = EXCLUDED.record_id,
    doctor_id = EXCLUDED.doctor_id,
    date = EXCLUDED.date,
    procedure_time = EXCLUDED.procedure_time,
    description = EXCLUDED.description,
    doctor_name = EXCLUDED.doctor_name,
    tooth_treated = EXCLUDED.tooth_treated,
    cost = EXCLUDED.cost,
    payment_amount = EXCLUDED.payment_amount,
    balance = EXCLUDED.balance,
    patient_signature_url = EXCLUDED.patient_signature_url,
    doctor_signature_url = EXCLUDED.doctor_signature_url,
    voided_at = EXCLUDED.voided_at,
    voided_by = EXCLUDED.voided_by,
    void_reason = EXCLUDED.void_reason,
    amends_evolution_id = EXCLUDED.amends_evolution_id,
    cie10_code = EXCLUDED.cie10_code,
    cie10_description = EXCLUDED.cie10_description,
    cups_code = EXCLUDED.cups_code,
    cups_description = EXCLUDED.cups_description,
    updated_at = now()
  RETURNING id INTO v_evolution_id;

  DELETE FROM public.evolution_materials_used
  WHERE evolution_id = v_evolution_id;

  IF p_materials IS NOT NULL AND jsonb_typeof(p_materials) = 'array' AND jsonb_array_length(p_materials) > 0 THEN
    FOR v_mat IN SELECT * FROM jsonb_array_elements(p_materials)
    LOOP
      INSERT INTO public.evolution_materials_used (
        id,
        evolution_id,
        material_id,
        material_name_snapshot,
        quantity_used,
        lot_number_snapshot,
        invima_snapshot,
        expiration_date_snapshot,
        manufacturing_date_snapshot,
        created_at
      ) VALUES (
        COALESCE(NULLIF(v_mat ->> 'id', '')::uuid, gen_random_uuid()),
        v_evolution_id,
        (v_mat ->> 'material_id')::uuid,
        v_mat ->> 'material_name_snapshot',
        COALESCE(NULLIF(v_mat ->> 'quantity_used', '')::numeric, 1.0),
        COALESCE(v_mat ->> 'lot_number_snapshot', ''),
        COALESCE(v_mat ->> 'invima_snapshot', ''),
        COALESCE(v_mat ->> 'expiration_date_snapshot', ''),
        v_mat ->> 'manufacturing_date_snapshot',
        now()
      );
    END LOOP;
  END IF;

  IF p_close_record THEN
    UPDATE public.clinical_records
    SET status = 'CERRADA',
        closed_at = now(),
        closed_by_doctor_id = v_doctor_id,
        closure_summary = p_closure_summary,
        closure_signature_url = p_closure_signature_url,
        updated_at = now()
    WHERE id = v_record_id;
  END IF;

  RETURN jsonb_build_object(
    'success', true,
    'evolution_id', v_evolution_id
  );
END;
$$;
