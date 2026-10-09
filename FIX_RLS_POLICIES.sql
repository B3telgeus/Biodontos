-- ============================================================================
-- BIODONTOS: CORRECCIÓN Y HABILITACIÓN DE POLÍTICAS RLS (Row Level Security)
-- Ejecutar en Supabase SQL Editor para solucionar el error de inserción en anamnesis
-- ============================================================================

-- 1. Actualizar la función is_doctor() para que sea resiliente:
-- Verifica tanto si el rol en metadata del JWT es 'DOCTOR', como si existe en public.doctors
CREATE OR REPLACE FUNCTION public.is_doctor()
RETURNS boolean AS $$
BEGIN
  RETURN (
    COALESCE(auth.jwt() -> 'user_metadata' ->> 'role', '') = 'DOCTOR'
    OR COALESCE(auth.jwt() ->> 'role', '') = 'service_role'
    OR EXISTS (
      SELECT 1 FROM public.doctors WHERE user_id::text = auth.jwt() ->> 'sub'
    )
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Asegurar RLS habilitado en tablas necesarias
ALTER TABLE public.anamnesis ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.treatments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.informed_consent_procedures ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tooth_states ENABLE ROW LEVEL SECURITY;

-- 3. POLÍTICAS PARA ANAMNESIS (Corrige: "new row violates row-level security policy for table anamnesis")
DROP POLICY IF EXISTS "Doctores gestionan anamnesis" ON public.anamnesis;
CREATE POLICY "Doctores gestionan anamnesis"
  ON public.anamnesis FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes pueden ver su propia anamnesis" ON public.anamnesis;
CREATE POLICY "Pacientes pueden ver su propia anamnesis"
  ON public.anamnesis FOR SELECT
  TO authenticated
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id::text = auth.jwt() ->> 'sub'));

DROP POLICY IF EXISTS "Pacientes pueden registrar su propia anamnesis" ON public.anamnesis;
CREATE POLICY "Pacientes pueden registrar su propia anamnesis"
  ON public.anamnesis FOR INSERT
  TO authenticated
  WITH CHECK (patient_id IN (SELECT id FROM public.patients WHERE user_id::text = auth.jwt() ->> 'sub'));

DROP POLICY IF EXISTS "Pacientes pueden actualizar su propia anamnesis" ON public.anamnesis;
CREATE POLICY "Pacientes pueden actualizar su propia anamnesis"
  ON public.anamnesis FOR UPDATE
  TO authenticated
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id::text = auth.jwt() ->> 'sub'))
  WITH CHECK (patient_id IN (SELECT id FROM public.patients WHERE user_id::text = auth.jwt() ->> 'sub'));

-- 4. POLÍTICAS PARA DOCTORS
DROP POLICY IF EXISTS "Doctores gestionan sus datos" ON public.doctors;
CREATE POLICY "Doctores gestionan sus datos"
  ON public.doctors FOR ALL
  TO authenticated
  USING (public.is_doctor() OR user_id::text = auth.jwt() ->> 'sub')
  WITH CHECK (public.is_doctor() OR user_id::text = auth.jwt() ->> 'sub');

DROP POLICY IF EXISTS "Usuarios autenticados pueden consultar doctores" ON public.doctors;
CREATE POLICY "Usuarios autenticados pueden consultar doctores"
  ON public.doctors FOR SELECT
  TO authenticated
  USING (true);

-- 5. POLÍTICAS PARA TREATMENTS
DROP POLICY IF EXISTS "Doctores gestionan tratamientos" ON public.treatments;
CREATE POLICY "Doctores gestionan tratamientos"
  ON public.treatments FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan sus tratamientos" ON public.treatments;
CREATE POLICY "Pacientes consultan sus tratamientos"
  ON public.treatments FOR SELECT
  TO authenticated
  USING (patient_id IN (
    SELECT id FROM public.patients WHERE user_id::text = auth.jwt() ->> 'sub'
  ));

-- 6. POLÍTICAS PARA PAYMENTS
DROP POLICY IF EXISTS "Doctores gestionan pagos" ON public.payments;
CREATE POLICY "Doctores gestionan pagos"
  ON public.payments FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan sus pagos" ON public.payments;
CREATE POLICY "Pacientes consultan sus pagos"
  ON public.payments FOR SELECT
  TO authenticated
  USING (treatment_id IN (
    SELECT t.id FROM public.treatments t
    JOIN public.patients p ON t.patient_id = p.id
    WHERE p.user_id::text = auth.jwt() ->> 'sub'
  ));

-- 7. POLÍTICAS PARA INFORMED_CONSENT_PROCEDURES
DROP POLICY IF EXISTS "Doctores gestionan procedimientos de consentimiento" ON public.informed_consent_procedures;
CREATE POLICY "Doctores gestionan procedimientos de consentimiento"
  ON public.informed_consent_procedures FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan sus procedimientos autorizados" ON public.informed_consent_procedures;
CREATE POLICY "Pacientes consultan sus procedimientos autorizados"
  ON public.informed_consent_procedures FOR SELECT
  TO authenticated
  USING (consent_id IN (
    SELECT id FROM public.informed_consents 
    WHERE patient_id IN (SELECT id FROM public.patients WHERE user_id::text = auth.jwt() ->> 'sub')
  ));

-- 8. POLÍTICAS PARA TOOTH_STATES
DROP POLICY IF EXISTS "Doctores gestionan estados dentales" ON public.tooth_states;
CREATE POLICY "Doctores gestionan estados dentales"
  ON public.tooth_states FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan estados dentales" ON public.tooth_states;
CREATE POLICY "Pacientes consultan estados dentales"
  ON public.tooth_states FOR SELECT
  TO authenticated
  USING (evolution_id IN (
    SELECT e.id FROM public.evolutions e
    JOIN public.clinical_records cr ON e.record_id = cr.id
    WHERE cr.patient_id IN (SELECT id FROM public.patients WHERE user_id::text = auth.jwt() ->> 'sub')
  ));
