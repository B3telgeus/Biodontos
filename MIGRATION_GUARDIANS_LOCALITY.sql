-- ============================================================================
-- PROYECTO: BIODONTOS
-- MIGRACIÓN MAESTRA: GUARDIANS, LOCALITY & RELACIÓN CON PACIENTES
-- MOTOR: PostgreSQL 15+ / Supabase
-- ============================================================================

-- 1. Crear tabla independiente para Acudientes (guardians)
CREATE TABLE IF NOT EXISTS public.guardians (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  first_name character varying(50) NOT NULL,
  last_name character varying(50) NOT NULL,
  phone character varying(20),
  address character varying(150),
  locality character varying(50),
  created_at timestamp with time zone DEFAULT now(),
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT guardians_pkey PRIMARY KEY (id)
);

-- 2. Añadir columnas a la tabla patients (locality y relación 1:N con guardians)
ALTER TABLE public.patients
  ADD COLUMN IF NOT EXISTS locality character varying(50),
  ADD COLUMN IF NOT EXISTS guardian_id uuid REFERENCES public.guardians(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS guardian_relationship character varying(50);

-- 3. Índices de rendimiento
CREATE INDEX IF NOT EXISTS idx_patients_guardian_id ON public.patients(guardian_id);
CREATE INDEX IF NOT EXISTS idx_patients_locality ON public.patients(locality);
CREATE INDEX IF NOT EXISTS idx_guardians_phone ON public.guardians(phone);

-- 4. Habilitar y configurar Row Level Security (RLS) en guardians
ALTER TABLE public.guardians ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan acudientes" ON public.guardians;
CREATE POLICY "Doctores gestionan acudientes"
  ON public.guardians FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan su acudiente" ON public.guardians;
CREATE POLICY "Pacientes consultan su acudiente"
  ON public.guardians FOR SELECT
  TO authenticated
  USING (
    id IN (SELECT guardian_id FROM public.patients WHERE user_id = auth.uid())
  );

DROP POLICY IF EXISTS "Pacientes gestionan su acudiente" ON public.guardians;
CREATE POLICY "Pacientes gestionan su acudiente"
  ON public.guardians FOR ALL
  TO authenticated
  USING (
    id IN (SELECT guardian_id FROM public.patients WHERE user_id = auth.uid())
  )
  WITH CHECK (
    id IN (SELECT guardian_id FROM public.patients WHERE user_id = auth.uid())
  );

-- 5. Migración de retrocompatibilidad (si ya existían datos en patients)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns 
    WHERE table_schema = 'public' 
      AND table_name = 'patients' 
      AND column_name = 'guardian_first_name'
  ) THEN
    -- Insertar en guardians desde patients
    INSERT INTO public.guardians (first_name, last_name, phone, address)
    SELECT DISTINCT
      guardian_first_name,
      COALESCE(guardian_last_name, ''),
      guardian_phone,
      guardian_address
    FROM public.patients
    WHERE guardian_first_name IS NOT NULL AND guardian_first_name <> ''
    ON CONFLICT DO NOTHING;

    -- Vincular guardian_id en patients
    UPDATE public.patients p
    SET guardian_id = g.id
    FROM public.guardians g
    WHERE p.guardian_first_name = g.first_name 
      AND COALESCE(p.guardian_last_name, '') = g.last_name
      AND p.guardian_id IS NULL;
  END IF;
END $$;
