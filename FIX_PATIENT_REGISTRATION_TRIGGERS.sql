-- ============================================================================
-- PROYECTO: BIODONTOS
-- AUDITORÍA & RESOLUCIÓN: REGISTRO DE PACIENTE NUEVO EN SUPABASE AUTH
-- PROBLEMA CORREGIDO:
--   "null value in column 'last_name' of relation 'patients' violates not-null constraint"
--   (Código Postgres 23502 al llamar /auth/v1/signup)
--
-- CAUSA RAÍZ:
--   1. El trigger preexistente 'on_auth_user_created' (handle_new_user) insertaba
--      en 'first_last_name' y 'second_last_name', omitiendo la columna 'last_name'.
--   2. En Supabase, la columna 'last_name' posee restricción NOT NULL sin DEFAULT.
--   3. Existía colisión entre 'on_auth_user_created' y 'on_auth_user_created_patient'.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. BLINDAR COLUMNAS DE APELLIDOS EN PUBLIC.PATIENTS (DEFAULTS SEGUROS)
-- ----------------------------------------------------------------------------
ALTER TABLE public.patients
  ADD COLUMN IF NOT EXISTS last_name character varying(100);

-- Garantizar que ninguna columna de apellidos falle por NOT NULL si se omite en un insert
ALTER TABLE public.patients ALTER COLUMN last_name SET DEFAULT '';
ALTER TABLE public.patients ALTER COLUMN first_last_name SET DEFAULT '';
ALTER TABLE public.patients ALTER COLUMN second_last_name SET DEFAULT '';

-- Actualizar registros donde last_name aún estuviera nulo o vacío
UPDATE public.patients
SET last_name = TRIM(CONCAT(COALESCE(first_last_name, ''), ' ', COALESCE(second_last_name, '')))
WHERE last_name IS NULL OR last_name = '';

-- ----------------------------------------------------------------------------
-- 2. TRIGGER DE AUTO-SINCRONIZACIÓN DE APELLIDOS EN PUBLIC.PATIENTS
--    (Mantiene sincronizados last_name <-> first_last_name / second_last_name)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.trg_sync_patient_surnames()
RETURNS trigger AS $$
BEGIN
  -- Si last_name viene provisto pero los campos heredados están vacíos:
  IF NEW.last_name IS NOT NULL AND NEW.last_name <> '' THEN
    IF NEW.first_last_name IS NULL OR NEW.first_last_name = '' THEN
      NEW.first_last_name := split_part(TRIM(NEW.last_name), ' ', 1);
    END IF;
    IF (NEW.second_last_name IS NULL OR NEW.second_last_name = '') AND position(' ' in TRIM(NEW.last_name)) > 0 THEN
      NEW.second_last_name := TRIM(SUBSTRING(TRIM(NEW.last_name) FROM position(' ' in TRIM(NEW.last_name)) + 1));
    END IF;
  -- Si first_last_name viene provisto pero last_name está vacío:
  ELSIF NEW.first_last_name IS NOT NULL AND NEW.first_last_name <> '' THEN
    NEW.last_name := TRIM(CONCAT(COALESCE(NEW.first_last_name, ''), ' ', COALESCE(NEW.second_last_name, '')));
  END IF;

  -- Blindaje final contra valores NULL
  IF NEW.last_name IS NULL THEN NEW.last_name := ''; END IF;
  IF NEW.first_last_name IS NULL THEN NEW.first_last_name := ''; END IF;
  IF NEW.second_last_name IS NULL THEN NEW.second_last_name := ''; END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS sync_patient_surnames ON public.patients;
CREATE TRIGGER sync_patient_surnames
  BEFORE INSERT OR UPDATE ON public.patients
  FOR EACH ROW EXECUTE FUNCTION public.trg_sync_patient_surnames();

-- ----------------------------------------------------------------------------
-- 3. UNIFICAR Y CORREGIR EL TRIGGER EN AUTH.USERS (CERO COLISIONES)
-- ----------------------------------------------------------------------------
-- Eliminar los triggers en conflicto
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP TRIGGER IF EXISTS on_auth_user_created_patient ON auth.users;
DROP FUNCTION IF EXISTS public.handle_new_user_patient();

-- Crear la función unificada definitiva para doctores y pacientes
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
DECLARE
  v_role text;
  v_first_name text;
  v_last_name text;
  v_phone text;
  v_dni text;
BEGIN
  v_role := COALESCE(NEW.raw_user_meta_data->>'role', 'PATIENT');
  v_first_name := COALESCE(NEW.raw_user_meta_data->>'first_name', split_part(COALESCE(NEW.raw_user_meta_data->>'display_name', 'Usuario'), ' ', 1));
  v_last_name := COALESCE(NEW.raw_user_meta_data->>'last_name', '');
  v_phone := COALESCE(NEW.raw_user_meta_data->>'phone', '');
  v_dni := COALESCE(NEW.raw_user_meta_data->>'dni_cedula', 'TEMP-' || SUBSTRING(NEW.id::text, 1, 8));

  -- 1. Registro de Doctor
  IF v_role = 'DOCTOR' THEN
    INSERT INTO public.doctors (
      id,
      user_id,
      first_name,
      last_name,
      email,
      phone,
      license_number,
      specialty
    ) VALUES (
      NEW.id,
      NEW.id,
      v_first_name,
      CASE WHEN v_last_name = '' THEN 'Profesional' ELSE v_last_name END,
      NEW.email,
      v_phone,
      COALESCE(NEW.raw_user_meta_data->>'license_number', 'PENDIENTE-' || SUBSTRING(NEW.id::text, 1, 8)),
      COALESCE(NEW.raw_user_meta_data->>'specialty', 'Odontología General')
    )
    ON CONFLICT (id) DO UPDATE SET
      user_id = EXCLUDED.user_id,
      email = EXCLUDED.email;

  -- 2. Registro de Paciente (Garantizando TODOS los campos obligatorios)
  ELSE
    INSERT INTO public.patients (
      id,
      user_id,
      dni_cedula,
      first_name,
      last_name,
      first_last_name,
      second_last_name,
      email,
      phone,
      birth_date,
      birth_place,
      eps,
      blood_group,
      rh_factor,
      consent_signed
    ) VALUES (
      NEW.id,
      NEW.id,
      v_dni,
      v_first_name,
      v_last_name,
      split_part(v_last_name, ' ', 1),
      CASE WHEN position(' ' in v_last_name) > 0 THEN TRIM(SUBSTRING(v_last_name FROM position(' ' in v_last_name) + 1)) ELSE '' END,
      NEW.email,
      v_phone,
      COALESCE((NEW.raw_user_meta_data->>'birth_date')::date, CURRENT_DATE),
      COALESCE(NEW.raw_user_meta_data->>'birth_place', 'No especificado'),
      COALESCE(NEW.raw_user_meta_data->>'eps', 'Particular'),
      COALESCE(NEW.raw_user_meta_data->>'blood_group', 'O'),
      COALESCE(NEW.raw_user_meta_data->>'rh_factor', '+'),
      true
    )
    ON CONFLICT (id) DO UPDATE SET
      user_id = EXCLUDED.user_id,
      email = EXCLUDED.email,
      last_name = CASE WHEN EXCLUDED.last_name <> '' THEN EXCLUDED.last_name ELSE public.patients.last_name END;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Reasociar el trigger único limpio a auth.users
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
