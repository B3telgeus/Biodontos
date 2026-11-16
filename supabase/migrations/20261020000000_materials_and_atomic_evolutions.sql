-- ============================================================================
-- BIODONTOS: MATERIALES CLÍNICOS, BUCKET DE AVATARES Y RPC EVOLUCIÓN ATÓMICA
-- Migración: 20261020000000_materials_and_atomic_evolutions.sql
-- ============================================================================

-- ============================================================================
-- 1. TABLA: public.clinical_materials (Control de Lotes e INVIMA)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.clinical_materials (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  doctor_id uuid REFERENCES public.doctors(id) ON DELETE SET NULL,
  name text NOT NULL,
  invima_registration text NOT NULL,
  batch_lot_number text NOT NULL,
  manufacturing_date date NOT NULL,
  expiration_date date NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_clinical_materials UNIQUE (invima_registration, batch_lot_number),
  CONSTRAINT chk_clinical_materials CHECK (expiration_date >= manufacturing_date)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_clinical_materials_doctor ON public.clinical_materials(doctor_id);
CREATE INDEX IF NOT EXISTS idx_clinical_materials_active ON public.clinical_materials(is_active);
CREATE INDEX IF NOT EXISTS idx_clinical_materials_name ON public.clinical_materials(name);

-- RLS en clinical_materials
ALTER TABLE public.clinical_materials ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan catálogo de materiales" ON public.clinical_materials;
CREATE POLICY "Doctores gestionan catálogo de materiales"
  ON public.clinical_materials FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

-- ============================================================================
-- 2. TABLA: public.evolution_materials_used (Materiales Consumidos)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.evolution_materials_used (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  evolution_id uuid NOT NULL REFERENCES public.evolutions(id) ON DELETE CASCADE,
  material_id uuid NOT NULL REFERENCES public.clinical_materials(id) ON DELETE RESTRICT,
  material_name_snapshot text,
  quantity_used numeric NOT NULL DEFAULT 1.0,
  lot_number_snapshot text NOT NULL,
  invima_snapshot text NOT NULL,
  expiration_date_snapshot text NOT NULL,
  manufacturing_date_snapshot text,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_evolution_materials_evolution ON public.evolution_materials_used(evolution_id);
CREATE INDEX IF NOT EXISTS idx_evolution_materials_material ON public.evolution_materials_used(material_id);

-- RLS en evolution_materials_used
ALTER TABLE public.evolution_materials_used ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan materiales de evolución" ON public.evolution_materials_used;
CREATE POLICY "Doctores gestionan materiales de evolución"
  ON public.evolution_materials_used FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan materiales de sus evoluciones" ON public.evolution_materials_used;
CREATE POLICY "Pacientes consultan materiales de sus evoluciones"
  ON public.evolution_materials_used FOR SELECT
  TO authenticated
  USING (evolution_id IN (
    SELECT e.id FROM public.evolutions e
    JOIN public.clinical_records cr ON e.record_id = cr.id
    JOIN public.patients p ON cr.patient_id = p.id
    WHERE p.user_id = auth.uid()
  ));

-- ============================================================================
-- 3. STORAGE BUCKET: doctor-avatars (Fotos de Perfil Profesional)
-- ============================================================================
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'storage' AND table_name = 'buckets') THEN
    INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
    VALUES (
      'doctor-avatars',
      'doctor-avatars',
      true,
      5242880, -- 5 MB
      ARRAY['image/jpeg', 'image/png', 'image/webp']::text[]
    )
    ON CONFLICT (id) DO UPDATE SET
      public = EXCLUDED.public,
      file_size_limit = EXCLUDED.file_size_limit,
      allowed_mime_types = EXCLUDED.allowed_mime_types;
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'storage' AND table_name = 'objects') THEN
    -- Lectura pública de avatares
    DROP POLICY IF EXISTS "Public read doctor-avatars" ON storage.objects;
    CREATE POLICY "Public read doctor-avatars"
      ON storage.objects FOR SELECT
      USING (bucket_id = 'doctor-avatars');

    -- Subida restringida al doctor propietario ({doctor_id}/...)
    DROP POLICY IF EXISTS "Doctors upload their own avatar" ON storage.objects;
    CREATE POLICY "Doctors upload their own avatar"
      ON storage.objects FOR INSERT
      TO authenticated
      WITH CHECK (
        bucket_id = 'doctor-avatars'
        AND (storage.foldername(name))[1] IN (
          SELECT id::text FROM public.doctors WHERE user_id = auth.uid()
        )
      );

    -- Modificación restringida al doctor propietario
    DROP POLICY IF EXISTS "Doctors update their own avatar" ON storage.objects;
    CREATE POLICY "Doctors update their own avatar"
      ON storage.objects FOR UPDATE
      TO authenticated
      USING (
        bucket_id = 'doctor-avatars'
        AND (storage.foldername(name))[1] IN (
          SELECT id::text FROM public.doctors WHERE user_id = auth.uid()
        )
      )
      WITH CHECK (
        bucket_id = 'doctor-avatars'
        AND (storage.foldername(name))[1] IN (
          SELECT id::text FROM public.doctors WHERE user_id = auth.uid()
        )
      );

    -- Eliminación restringida al doctor propietario
    DROP POLICY IF EXISTS "Doctors delete their own avatar" ON storage.objects;
    CREATE POLICY "Doctors delete their own avatar"
      ON storage.objects FOR DELETE
      TO authenticated
      USING (
        bucket_id = 'doctor-avatars'
        AND (storage.foldername(name))[1] IN (
          SELECT id::text FROM public.doctors WHERE user_id = auth.uid()
        )
      );
  END IF;
END $$;

-- ============================================================================
-- 4. RPC TRANSACCIONAL: public.save_evolution_with_materials
-- Operación atómica de guardado de evolución, reemplazo idempotente de materiales
-- consumidos y cierre opcional de historia clínica.
-- ============================================================================
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
  -- 1. Validar autenticación y rol de doctor
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated' USING ERRCODE = '28000';
  END IF;

  IF NOT public.is_doctor() THEN
    RAISE EXCEPTION 'Forbidden: only doctors can save evolutions' USING ERRCODE = '42501';
  END IF;

  -- 2. Resolver doctor_id correspondiente al auth.uid()
  SELECT id INTO v_doctor_id
  FROM public.doctors
  WHERE user_id = auth.uid()
  LIMIT 1;

  IF v_doctor_id IS NULL THEN
    RAISE EXCEPTION 'Doctor profile not found for authenticated user' USING ERRCODE = 'P0002';
  END IF;

  -- 3. Extraer y validar identificadores de evolución e historia
  v_evolution_id := COALESCE(NULLIF(p_evolution ->> 'id', '')::uuid, gen_random_uuid());
  v_record_id := (p_evolution ->> 'record_id')::uuid;

  IF v_record_id IS NULL THEN
    RAISE EXCEPTION 'record_id is required in evolution payload' USING ERRCODE = '23502';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.clinical_records WHERE id = v_record_id) THEN
    RAISE EXCEPTION 'Clinical record with id % not found', v_record_id USING ERRCODE = 'P0002';
  END IF;

  -- 4. Upsert en public.evolutions
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
    created_at,
    updated_at
  ) VALUES (
    v_evolution_id,
    v_record_id,
    COALESCE(NULLIF(p_evolution ->> 'doctor_id', '')::uuid, v_doctor_id),
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
    updated_at = now()
  RETURNING id INTO v_evolution_id;

  -- 5. Idempotencia: Eliminar materiales asociados existentes antes de re-insertar
  DELETE FROM public.evolution_materials_used
  WHERE evolution_id = v_evolution_id;

  -- 6. Insertar nuevos materiales consumidos si fueron provistos
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

  -- 7. Cierre atómico de historia clínica si se solicita
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

  -- 8. Retornar confirmación estructurada
  RETURN jsonb_build_object(
    'success', true,
    'evolution_id', v_evolution_id
  );
END;
$$;
