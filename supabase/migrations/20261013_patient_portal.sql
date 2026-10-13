-- ============================================================================
-- BIODONTOS: MIGRACIÓN PORTAL DEL PACIENTE (CITAS, DOCTORES, PAGOS Y RLS)
-- ============================================================================

-- 1. Ampliar tabla doctors con bio y foto para el perfil público del paciente
ALTER TABLE public.doctors 
  ADD COLUMN IF NOT EXISTS bio text,
  ADD COLUMN IF NOT EXISTS photo_url text;

-- 2. Crear tabla doctor_availability para los horarios semanales del doctor
CREATE TABLE IF NOT EXISTS public.doctor_availability (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  doctor_id uuid NOT NULL REFERENCES public.doctors(id) ON DELETE CASCADE,
  day_of_week smallint NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1 = Lunes, 7 = Domingo (ISO)
  start_time time NOT NULL,
  end_time time NOT NULL CHECK (end_time > start_time),
  slot_minutes smallint NOT NULL DEFAULT 30 CHECK (slot_minutes BETWEEN 10 AND 240),
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone DEFAULT now()
);

-- Habilitar RLS en doctor_availability
ALTER TABLE public.doctor_availability ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Todos los autenticados pueden ver disponibilidad de doctores" ON public.doctor_availability;
CREATE POLICY "Todos los autenticados pueden ver disponibilidad de doctores"
  ON public.doctor_availability FOR SELECT
  TO authenticated
  USING (true);

DROP POLICY IF EXISTS "Doctores gestionan su propia disponibilidad" ON public.doctor_availability;
CREATE POLICY "Doctores gestionan su propia disponibilidad"
  ON public.doctor_availability FOR ALL
  TO authenticated
  USING (
    doctor_id IN (SELECT id FROM public.doctors WHERE user_id::text = auth.jwt() ->> 'sub')
    OR public.is_doctor()
  )
  WITH CHECK (
    doctor_id IN (SELECT id FROM public.doctors WHERE user_id::text = auth.jwt() ->> 'sub')
    OR public.is_doctor()
  );

-- 3. Índice único para evitar doble reserva concurrente
CREATE UNIQUE INDEX IF NOT EXISTS ux_appointments_doctor_slot
  ON public.appointments (doctor_id, start_time) 
  WHERE status <> 'CANCELLED';

-- 4. RLS en appointments para pacientes (lectura de sus propias citas)
DROP POLICY IF EXISTS "Pacientes consultan sus propias citas" ON public.appointments;
CREATE POLICY "Pacientes consultan sus propias citas"
  ON public.appointments FOR SELECT
  TO authenticated
  USING (
    patient_id IN (SELECT id FROM public.patients WHERE user_id::text = auth.jwt() ->> 'sub')
  );

-- 5. RPC segura: Obtener slots ocupados de un doctor (sin exponer datos de pacientes - Cumplimiento HIPAA)
CREATE OR REPLACE FUNCTION public.get_doctor_busy_slots(
  p_doctor_id uuid,
  p_from timestamp with time zone,
  p_to timestamp with time zone
)
RETURNS TABLE (
  start_time timestamp with time zone,
  end_time timestamp with time zone
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  RETURN QUERY
  SELECT a.start_time, a.end_time
  FROM public.appointments a
  WHERE a.doctor_id = p_doctor_id
    AND a.status <> 'CANCELLED'
    AND a.start_time >= p_from
    AND a.start_time <= p_to;
END;
$$;

-- 6. RPC: Agendar cita por paciente con validación atómica
CREATE OR REPLACE FUNCTION public.patient_book_appointment(
  p_doctor_id uuid,
  p_start_time timestamp with time zone,
  p_reason text
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_patient_id uuid;
  v_end_time timestamp with time zone;
  v_new_appointment record;
BEGIN
  -- Obtener el paciente asociado al usuario autenticado
  SELECT id INTO v_patient_id
  FROM public.patients
  WHERE user_id::text = auth.jwt() ->> 'sub'
  LIMIT 1;

  IF v_patient_id IS NULL THEN
    RETURN jsonb_build_object('success', false, 'error', 'PATIENT_PROFILE_NOT_FOUND');
  END IF;

  -- Validar que la fecha sea futura (mínimo 2 horas de anticipación)
  IF p_start_time < (now() + interval '2 hours') THEN
    RETURN jsonb_build_object('success', false, 'error', 'IN_PAST_OR_WINDOW_CLOSED');
  END IF;

  -- Calcular fin aproximado (30 min por defecto)
  v_end_time := p_start_time + interval '30 minutes';

  -- Validar colisión de horario
  IF EXISTS (
    SELECT 1 FROM public.appointments
    WHERE doctor_id = p_doctor_id
      AND status <> 'CANCELLED'
      AND (
        (start_time <= p_start_time AND end_time > p_start_time) OR
        (start_time < v_end_time AND end_time >= v_end_time) OR
        (start_time >= p_start_time AND end_time <= v_end_time)
      )
  ) THEN
    RETURN jsonb_build_object('success', false, 'error', 'SLOT_TAKEN');
  END IF;

  -- Insertar cita con estado PENDING (confirmation_status = false)
  INSERT INTO public.appointments (
    patient_id,
    doctor_id,
    start_time,
    end_time,
    status,
    confirmation_status,
    reason,
    created_at,
    updated_at
  ) VALUES (
    v_patient_id,
    p_doctor_id,
    p_start_time,
    v_end_time,
    'SCHEDULED',
    false,
    p_reason,
    now(),
    now()
  )
  RETURNING * INTO v_new_appointment;

  RETURN jsonb_build_object(
    'success', true,
    'appointment_id', v_new_appointment.id,
    'status', 'PENDING'
  );
END;
$$;

-- 7. RPC: Reagendar cita por paciente (ventana mínima de 2 horas)
CREATE OR REPLACE FUNCTION public.patient_reschedule_appointment(
  p_appointment_id uuid,
  p_new_start_time timestamp with time zone
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_patient_id uuid;
  v_appointment record;
  v_new_end_time timestamp with time zone;
BEGIN
  -- Obtener el paciente asociado al usuario autenticado
  SELECT id INTO v_patient_id
  FROM public.patients
  WHERE user_id::text = auth.jwt() ->> 'sub'
  LIMIT 1;

  SELECT * INTO v_appointment
  FROM public.appointments
  WHERE id = p_appointment_id;

  IF v_appointment.id IS NULL THEN
    RETURN jsonb_build_object('success', false, 'error', 'APPOINTMENT_NOT_FOUND');
  END IF;

  IF v_appointment.patient_id <> v_patient_id THEN
    RETURN jsonb_build_object('success', false, 'error', 'NOT_OWNER');
  END IF;

  -- Ventana de 2 horas sobre la cita original
  IF v_appointment.start_time < (now() + interval '2 hours') THEN
    RETURN jsonb_build_object('success', false, 'error', 'MODIFICATION_WINDOW_CLOSED');
  END IF;

  IF v_appointment.status = 'CANCELLED' THEN
    RETURN jsonb_build_object('success', false, 'error', 'ALREADY_CANCELLED');
  END IF;

  -- Validar que la nueva fecha sea a futuro
  IF p_new_start_time < (now() + interval '2 hours') THEN
    RETURN jsonb_build_object('success', false, 'error', 'IN_PAST_OR_WINDOW_CLOSED');
  END IF;

  v_new_end_time := p_new_start_time + interval '30 minutes';

  -- Validar colisión en el nuevo horario
  IF EXISTS (
    SELECT 1 FROM public.appointments
    WHERE doctor_id = v_appointment.doctor_id
      AND id <> p_appointment_id
      AND status <> 'CANCELLED'
      AND (
        (start_time <= p_new_start_time AND end_time > p_new_start_time) OR
        (start_time < v_new_end_time AND end_time >= v_new_end_time)
      )
  ) THEN
    RETURN jsonb_build_object('success', false, 'error', 'SLOT_TAKEN');
  END IF;

  -- Actualizar y restablecer confirmation_status a false (pendiente de confirmar por doctor)
  UPDATE public.appointments
  SET start_time = p_new_start_time,
      end_time = v_new_end_time,
      confirmation_status = false,
      updated_at = now()
  WHERE id = p_appointment_id;

  RETURN jsonb_build_object('success', true);
END;
$$;

-- 8. RPC: Cancelar cita por paciente (ventana mínima de 2 horas)
CREATE OR REPLACE FUNCTION public.patient_cancel_appointment(
  p_appointment_id uuid,
  p_reason text DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_patient_id uuid;
  v_appointment record;
BEGIN
  SELECT id INTO v_patient_id
  FROM public.patients
  WHERE user_id::text = auth.jwt() ->> 'sub'
  LIMIT 1;

  SELECT * INTO v_appointment
  FROM public.appointments
  WHERE id = p_appointment_id;

  IF v_appointment.id IS NULL THEN
    RETURN jsonb_build_object('success', false, 'error', 'APPOINTMENT_NOT_FOUND');
  END IF;

  IF v_appointment.patient_id <> v_patient_id THEN
    RETURN jsonb_build_object('success', false, 'error', 'NOT_OWNER');
  END IF;

  IF v_appointment.status = 'CANCELLED' THEN
    RETURN jsonb_build_object('success', false, 'error', 'ALREADY_CANCELLED');
  END IF;

  -- Regla de 2 horas
  IF v_appointment.start_time < (now() + interval '2 hours') THEN
    RETURN jsonb_build_object('success', false, 'error', 'MODIFICATION_WINDOW_CLOSED');
  END IF;

  UPDATE public.appointments
  SET status = 'CANCELLED',
      reason = COALESCE(p_reason, reason),
      updated_at = now()
  WHERE id = p_appointment_id;

  RETURN jsonb_build_object('success', true);
END;
$$;

-- 9. Seed de disponibilidad por defecto (Lunes a Viernes 08:00 - 12:00 y 14:00 - 18:00) para doctores sin horario
INSERT INTO public.doctor_availability (doctor_id, day_of_week, start_time, end_time, slot_minutes)
SELECT d.id, day_series.day_of_week, '08:00:00'::time, '12:00:00'::time, 30
FROM public.doctors d
CROSS JOIN (VALUES (1), (2), (3), (4), (5)) AS day_series(day_of_week)
WHERE NOT EXISTS (
  SELECT 1 FROM public.doctor_availability da WHERE da.doctor_id = d.id
);

INSERT INTO public.doctor_availability (doctor_id, day_of_week, start_time, end_time, slot_minutes)
SELECT d.id, day_series.day_of_week, '14:00:00'::time, '18:00:00'::time, 30
FROM public.doctors d
CROSS JOIN (VALUES (1), (2), (3), (4), (5)) AS day_series(day_of_week)
WHERE NOT EXISTS (
  SELECT 1 FROM public.doctor_availability da 
  WHERE da.doctor_id = d.id AND da.day_of_week = day_series.day_of_week AND da.start_time = '14:00:00'::time
);
