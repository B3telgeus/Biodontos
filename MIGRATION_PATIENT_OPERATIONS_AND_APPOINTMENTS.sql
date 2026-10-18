-- ============================================================================
-- PROYECTO: BIODONTOS
-- MIGRACIÓN MAESTRA AUDITADA: SEGURIDAD RLS, PREVENCIÓN DE FUGA PHI,
-- RPC DE DISPONIBILIDAD HORARIA Y BLINDAJE DE CITAS
-- MOTOR: PostgreSQL 15+ / Supabase
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. TABLA: PATIENTS (Garantía de Columnas y Retrocompatibilidad)
-- ----------------------------------------------------------------------------
ALTER TABLE public.patients
  ADD COLUMN IF NOT EXISTS last_name character varying(100),
  ADD COLUMN IF NOT EXISTS locality character varying(50),
  ADD COLUMN IF NOT EXISTS guardian_id uuid REFERENCES public.guardians(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS guardian_relationship character varying(50);

-- Sincronizar last_name en registros preexistentes
UPDATE public.patients
SET last_name = TRIM(CONCAT(COALESCE(first_last_name, ''), ' ', COALESCE(second_last_name, '')))
WHERE (last_name IS NULL OR last_name = '') AND (first_last_name <> '' OR second_last_name <> '');

-- ----------------------------------------------------------------------------
-- 2. TABLA: DOCTORS (Garantía de Especialistas Clínicos y Plantilla Inicial)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.doctors (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  user_id uuid UNIQUE,
  first_name text NOT NULL,
  last_name text NOT NULL,
  email text NOT NULL UNIQUE,
  phone text,
  license_number text NOT NULL UNIQUE,
  specialty text NOT NULL DEFAULT 'Odontología General',
  signature_stamp_url text,
  created_at timestamp with time zone DEFAULT now(),
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT doctors_pkey PRIMARY KEY (id)
);

-- Sembrar especialistas odontológicos con UUIDs garantizados para agendamiento
INSERT INTO public.doctors (id, first_name, last_name, specialty, email, license_number)
VALUES 
  ('11111111-1111-1111-1111-111111111111', 'Laura', 'Gómez', 'Odontología General', 'laura.gomez@biodontos.com', 'OD-10492'),
  ('22222222-2222-2222-2222-222222222222', 'Carlos', 'Mendoza', 'Ortodoncia & Estética', 'carlos.mendoza@biodontos.com', 'OD-20381'),
  ('33333333-3333-3333-3333-333333333333', 'Valentina', 'Ríos', 'Endodoncia & Periodoncia', 'valentina.rios@biodontos.com', 'OD-30918')
ON CONFLICT (id) DO UPDATE SET
  first_name = EXCLUDED.first_name,
  last_name = EXCLUDED.last_name,
  specialty = EXCLUDED.specialty,
  email = EXCLUDED.email;

-- ----------------------------------------------------------------------------
-- 3. TABLA: APPOINTMENTS (Citas, Reagendamiento, Cancelación y Confirmación)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.appointments (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  patient_id uuid NOT NULL,
  doctor_id uuid,
  origin_evolution_id uuid,
  start_time timestamp with time zone NOT NULL,
  end_time timestamp with time zone,
  status text NOT NULL DEFAULT 'SCHEDULED',
  confirmation_status boolean NOT NULL DEFAULT false,
  reason text,
  created_at timestamp with time zone DEFAULT now(),
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT appointments_pkey PRIMARY KEY (id),
  CONSTRAINT appointments_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patients(id) ON DELETE CASCADE,
  CONSTRAINT appointments_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctors(id) ON DELETE SET NULL
);

-- Índices de alto rendimiento
CREATE INDEX IF NOT EXISTS idx_appointments_patient_status_time ON public.appointments(patient_id, status, start_time);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date ON public.appointments(doctor_id, start_time);

-- ----------------------------------------------------------------------------
-- 4. PREVENCIÓN DE COLISIÓN (Anti-Double Booking a Nivel de Base de Datos)
-- ----------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS idx_appointments_doctor_slot_unique 
ON public.appointments (doctor_id, start_time) 
WHERE status <> 'CANCELLED';

-- ----------------------------------------------------------------------------
-- 5. FUNCIÓN RPC SEGURA: DISPONIBILIDAD HORARIA (CERO FUGA DE PHI - HALLAZGO S1)
-- Retorna ÚNICAMENTE los timestamps ocupados del doctor para una fecha.
-- No transfiere nombres, patient_id, notas ni motivos de consulta confidenciales.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_doctor_busy_slots(p_doctor_id uuid, p_date date)
RETURNS TABLE(start_time timestamp with time zone)
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
  SELECT a.start_time
  FROM public.appointments a
  WHERE a.doctor_id = p_doctor_id
    AND a.status <> 'CANCELLED'
    AND (a.start_time AT TIME ZONE 'America/Bogota')::date = p_date;
$$;

GRANT EXECUTE ON FUNCTION public.get_doctor_busy_slots(uuid, date) TO authenticated;

-- ----------------------------------------------------------------------------
-- 6. TRIGGER DE BLINDAJE: PREVENCIÓN DE AUTOCONFIRMACIÓN (HALLAZGOS S2 & F2)
-- Impide que un paciente altere confirmation_status sin ser doctor, y
-- fuerza confirmation_status = false si el paciente reagenda la cita.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.trg_validate_appointment_update()
RETURNS trigger AS $$
BEGIN
  -- Regla 1: Solo un doctor puede confirmar o alterar el estado de confirmación
  IF NEW.confirmation_status IS DISTINCT FROM OLD.confirmation_status AND NOT public.is_doctor() THEN
    RAISE EXCEPTION 'Solo un profesional odontólogo tiene autorización para confirmar o alterar el estado de aprobación de citas.';
  END IF;

  -- Regla 2: Si el paciente modifica la fecha/hora de inicio (reagendamiento), reiniciar confirmación
  IF NEW.start_time IS DISTINCT FROM OLD.start_time AND NOT public.is_doctor() THEN
    NEW.confirmation_status := false;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS check_appointment_update ON public.appointments;
CREATE TRIGGER check_appointment_update
  BEFORE UPDATE ON public.appointments
  FOR EACH ROW EXECUTE FUNCTION public.trg_validate_appointment_update();

-- ----------------------------------------------------------------------------
-- 7. POLÍTICAS RLS ESTRICTAS (ROW LEVEL SECURITY)
-- ----------------------------------------------------------------------------

-- A. Habilitar RLS
ALTER TABLE public.patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

-- B. Políticas en PATIENTS (Registro, Consulta y Actualización de Perfil)
DROP POLICY IF EXISTS "Doctores tienen acceso completo a pacientes" ON public.patients;
CREATE POLICY "Doctores tienen acceso completo a pacientes"
  ON public.patients FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes pueden registrar su propio perfil" ON public.patients;
CREATE POLICY "Pacientes pueden registrar su propio perfil"
  ON public.patients FOR INSERT
  TO authenticated
  WITH CHECK (
    user_id = auth.uid() 
    OR id::text = auth.jwt() ->> 'sub'
    OR public.is_doctor()
  );

DROP POLICY IF EXISTS "Pacientes pueden ver su propio perfil" ON public.patients;
CREATE POLICY "Pacientes pueden ver su propio perfil"
  ON public.patients FOR SELECT
  TO authenticated
  USING (
    user_id = auth.uid() 
    OR id::text = auth.jwt() ->> 'sub'
    OR public.is_doctor()
  );

DROP POLICY IF EXISTS "Pacientes pueden actualizar sus datos personales" ON public.patients;
CREATE POLICY "Pacientes pueden actualizar sus datos personales"
  ON public.patients FOR UPDATE
  TO authenticated
  USING (
    user_id = auth.uid() 
    OR id::text = auth.jwt() ->> 'sub'
    OR public.is_doctor()
  )
  WITH CHECK (
    user_id = auth.uid() 
    OR id::text = auth.jwt() ->> 'sub'
    OR public.is_doctor()
  );

-- C. Políticas en DOCTORS (Consulta de staff)
DROP POLICY IF EXISTS "Usuarios autenticados pueden consultar doctores" ON public.doctors;
CREATE POLICY "Usuarios autenticados pueden consultar doctores"
  ON public.doctors FOR SELECT
  TO authenticated
  USING (true);

-- D. Políticas en APPOINTMENTS (Estrictas y Seguras)
DROP POLICY IF EXISTS "Doctores gestionan citas" ON public.appointments;
CREATE POLICY "Doctores gestionan citas"
  ON public.appointments FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

-- ELIMINACIÓN DE LA POLÍTICA VULNERABLE QUE PERMITÍA A PACIENTES VER CITAS AJENAS
DROP POLICY IF EXISTS "Usuarios autenticados verifican disponibilidad de citas" ON public.appointments;
DROP POLICY IF EXISTS "Pacientes consultan y solicitan sus citas" ON public.appointments;
DROP POLICY IF EXISTS "Pacientes consultan sus propias citas" ON public.appointments;

-- Consulta ESTRICTA: El paciente SOLO ve sus propias citas completas
CREATE POLICY "Pacientes consultan sus propias citas"
  ON public.appointments FOR SELECT
  TO authenticated
  USING (
    patient_id IN (
      SELECT id FROM public.patients 
      WHERE user_id = auth.uid() OR id::text = auth.jwt() ->> 'sub'
    )
    OR patient_id::text = auth.jwt() ->> 'sub'
    OR public.is_doctor()
  );

-- Agendamiento ESTRICTO: El paciente solo puede insertar citas a su nombre
DROP POLICY IF EXISTS "Pacientes pueden agendar citas" ON public.appointments;
CREATE POLICY "Pacientes pueden agendar citas"
  ON public.appointments FOR INSERT
  TO authenticated
  WITH CHECK (
    patient_id IN (
      SELECT id FROM public.patients 
      WHERE user_id = auth.uid() OR id::text = auth.jwt() ->> 'sub'
    )
    OR patient_id::text = auth.jwt() ->> 'sub'
    OR public.is_doctor()
  );

-- Reagendamiento y Cancelación ESTRICTOS:
DROP POLICY IF EXISTS "Pacientes pueden reagendar o cancelar sus citas" ON public.appointments;
CREATE POLICY "Pacientes pueden reagendar o cancelar sus citas"
  ON public.appointments FOR UPDATE
  TO authenticated
  USING (
    patient_id IN (
      SELECT id FROM public.patients 
      WHERE user_id = auth.uid() OR id::text = auth.jwt() ->> 'sub'
    )
    OR patient_id::text = auth.jwt() ->> 'sub'
    OR public.is_doctor()
  )
  WITH CHECK (
    patient_id IN (
      SELECT id FROM public.patients 
      WHERE user_id = auth.uid() OR id::text = auth.jwt() ->> 'sub'
    )
    OR patient_id::text = auth.jwt() ->> 'sub'
    OR public.is_doctor()
  );

-- ----------------------------------------------------------------------------
-- 7. SINCRONIZACIÓN AUTOMÁTICA DE REGISTRO DE PACIENTE (Hallazgo S4)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user_patient()
RETURNS trigger AS $$
BEGIN
  IF (NEW.raw_user_meta_data->>'role') = 'PATIENT' OR (NEW.raw_user_meta_data->>'role') IS NULL THEN
    INSERT INTO public.patients (id, user_id, email, first_name, last_name, consent_signed)
    VALUES (
      NEW.id,
      NEW.id,
      NEW.email,
      COALESCE(NEW.raw_user_meta_data->>'first_name', split_part(COALESCE(NEW.raw_user_meta_data->>'display_name', 'Paciente'), ' ', 1)),
      COALESCE(NEW.raw_user_meta_data->>'last_name', ''),
      true
    )
    ON CONFLICT (id) DO UPDATE SET
      user_id = EXCLUDED.user_id,
      email = EXCLUDED.email;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created_patient ON auth.users;
CREATE TRIGGER on_auth_user_created_patient
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user_patient();

