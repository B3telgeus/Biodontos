-- ============================================================================
-- MIGRACIÓN: CORRECCIÓN DE LLAVE FORÁNEA EN PERIODONTOGRAMA Y DOCTOR POR DEFECTO
-- Archivo: MIGRATION_PERIODONTAL_DOCTOR_FIX.sql
-- Ejecutar en: Supabase SQL Editor
-- ============================================================================

-- 1. Garantizar que la tabla public.doctors tenga al menos un especialista por defecto
INSERT INTO public.doctors (id, first_name, last_name, specialty, email, license_number)
VALUES (
  '11111111-1111-1111-1111-111111111111', 
  'Dra. Erika', 
  'Bejarano Beltrán', 
  'Odontología General & Operatoria', 
  'erika.bejarano@biodontos.com', 
  'OD-98234-COL'
)
ON CONFLICT (id) DO UPDATE SET
  first_name = EXCLUDED.first_name,
  last_name = EXCLUDED.last_name,
  specialty = EXCLUDED.specialty,
  license_number = EXCLUDED.license_number;

-- 2. Permitir que doctor_id sea NULLABLE en periodontal_examinations
-- Esto evita bloqueos clínicos si la admisión se realiza en modo de triaje o contingencia
ALTER TABLE public.periodontal_examinations 
  ALTER COLUMN doctor_id DROP NOT NULL;

-- 3. Normalizar la restricción de llave foránea (eliminando el typo 'preiodontal' si existe)
ALTER TABLE public.periodontal_examinations 
  DROP CONSTRAINT IF EXISTS preiodontal_examinations_doctor_id_fkey;

ALTER TABLE public.periodontal_examinations 
  DROP CONSTRAINT IF EXISTS periodontal_examinations_doctor_id_fkey;

ALTER TABLE public.periodontal_examinations
  ADD CONSTRAINT periodontal_examinations_doctor_id_fkey
  FOREIGN KEY (doctor_id) REFERENCES public.doctors(id) ON DELETE SET NULL;

-- 4. Notificar a PostgREST para refrescar el esquema de la API
NOTIFY pgrst, 'reload schema';
