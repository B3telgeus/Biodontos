-- ============================================================================
-- BIODONTOS: ESQUEMA BASE DEL SISTEMA CLÍNICO ODONTOLÓGICO
-- Migración: 20261001000000_base_schema.sql
-- Tablas: doctors, guardians, patients, appointments, clinical_records,
--         anamnesis, evolutions, tooth_states, periodontal_examinations,
--         periodontal_sites, informed_consents, informed_consent_procedures,
--         clinical_audit_log, treatments, payments.
-- Funciones: public.is_doctor(), public.handle_new_user()
-- Trigger: on_auth_user_created ON auth.users
-- ============================================================================

-- Extensión pgcrypto para generación de UUIDs si no existe
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. TABLA: public.doctors
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.doctors (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid UNIQUE REFERENCES auth.users(id) ON DELETE SET NULL,
  first_name text NOT NULL,
  last_name text NOT NULL,
  email text NOT NULL DEFAULT '',
  phone text,
  license_number text NOT NULL DEFAULT '',
  specialty text NOT NULL DEFAULT 'Odontología General',
  bio text,
  photo_url text,
  clinic_address text,
  services text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 2. FUNCIÓN HELPER: public.is_doctor()
-- ============================================================================
CREATE OR REPLACE FUNCTION public.is_doctor()
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RETURN false;
  END IF;
  RETURN EXISTS (
    SELECT 1 FROM public.doctors WHERE user_id = auth.uid()
  );
END;
$$;

-- ============================================================================
-- 3. TABLA: public.guardians
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.guardians (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  first_name text NOT NULL,
  last_name text NOT NULL,
  phone text,
  address text,
  locality text,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 4. TABLA: public.patients
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.patients (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid UNIQUE REFERENCES auth.users(id) ON DELETE SET NULL,
  dni_cedula text NOT NULL UNIQUE,
  first_name text NOT NULL,
  last_name text,
  first_last_name text NOT NULL DEFAULT '',
  second_last_name text NOT NULL DEFAULT '',
  document_type_id integer NOT NULL DEFAULT 1,
  birth_date text NOT NULL DEFAULT '',
  birth_place text NOT NULL DEFAULT 'No especificado',
  eps text NOT NULL DEFAULT 'Particular',
  occupation text,
  marital_status text,
  blood_group text NOT NULL DEFAULT 'O',
  rh_factor text NOT NULL DEFAULT '+',
  phone text NOT NULL DEFAULT '',
  email text,
  address text,
  locality text,
  guardian_id uuid REFERENCES public.guardians(id) ON DELETE SET NULL,
  guardian_relationship text,
  guardian_first_name text,
  guardian_last_name text,
  guardian_phone text,
  guardian_address text,
  last_dental_visit text,
  consultation_reason text,
  current_consultation_reason text,
  illness_history text,
  critical_allergies text NOT NULL DEFAULT 'Ninguna',
  allergies_description text,
  consent_signed boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 5. TRIGGER ON auth.users -> handle_new_user()
-- ============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_role text;
  v_first_name text;
  v_last_name text;
  v_phone text;
  v_dni text;
  v_first_last_name text;
  v_second_last_name text;
  v_license_number text;
  v_specialty text;
BEGIN
  v_role := UPPER(COALESCE(NEW.raw_user_meta_data ->> 'role', 'PATIENT'));
  v_first_name := COALESCE(NEW.raw_user_meta_data ->> 'first_name', '');
  v_last_name := COALESCE(NEW.raw_user_meta_data ->> 'last_name', '');
  v_phone := NEW.raw_user_meta_data ->> 'phone';

  IF v_role = 'DOCTOR' THEN
    v_license_number := COALESCE(NEW.raw_user_meta_data ->> 'license_number', '');
    v_specialty := COALESCE(NEW.raw_user_meta_data ->> 'specialty', 'Odontología General');

    INSERT INTO public.doctors (
      user_id,
      first_name,
      last_name,
      email,
      phone,
      license_number,
      specialty
    ) VALUES (
      NEW.id,
      v_first_name,
      v_last_name,
      COALESCE(NEW.email, ''),
      v_phone,
      v_license_number,
      v_specialty
    )
    ON CONFLICT (user_id) DO UPDATE SET
      first_name = EXCLUDED.first_name,
      last_name = EXCLUDED.last_name,
      email = EXCLUDED.email,
      phone = COALESCE(EXCLUDED.phone, public.doctors.phone),
      license_number = EXCLUDED.license_number,
      specialty = EXCLUDED.specialty,
      updated_at = now();

  ELSE
    -- PATIENT
    v_dni := COALESCE(NEW.raw_user_meta_data ->> 'dni_cedula', NEW.id::text);
    v_first_last_name := split_part(v_last_name, ' ', 1);
    IF position(' ' in v_last_name) > 0 THEN
      v_second_last_name := substring(v_last_name from position(' ' in v_last_name) + 1);
    ELSE
      v_second_last_name := '';
    END IF;

    INSERT INTO public.patients (
      user_id,
      dni_cedula,
      first_name,
      last_name,
      first_last_name,
      second_last_name,
      phone,
      email,
      critical_allergies,
      consent_signed
    ) VALUES (
      NEW.id,
      v_dni,
      v_first_name,
      v_last_name,
      v_first_last_name,
      v_second_last_name,
      COALESCE(v_phone, ''),
      NEW.email,
      'Ninguna',
      true
    )
    ON CONFLICT (dni_cedula) DO UPDATE SET
      user_id = EXCLUDED.user_id,
      first_name = EXCLUDED.first_name,
      last_name = EXCLUDED.last_name,
      phone = EXCLUDED.phone,
      email = EXCLUDED.email,
      updated_at = now();
  END IF;

  RETURN NEW;
EXCEPTION
  WHEN OTHERS THEN
    RAISE WARNING 'handle_new_user error: %', SQLERRM;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================================
-- 6. TABLA: public.appointments
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.appointments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id uuid NOT NULL REFERENCES public.patients(id) ON DELETE CASCADE,
  doctor_id uuid REFERENCES public.doctors(id) ON DELETE SET NULL,
  origin_evolution_id uuid,
  start_time timestamptz NOT NULL,
  end_time timestamptz,
  status text NOT NULL DEFAULT 'SCHEDULED',
  confirmation_status boolean NOT NULL DEFAULT false,
  reason text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 7. TABLA: public.clinical_records
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.clinical_records (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id uuid NOT NULL REFERENCES public.patients(id) ON DELETE CASCADE,
  dentition_type text NOT NULL DEFAULT 'ADULT',
  main_diagnosis text,
  related_diagnoses text,
  complementary_exams text,
  status text NOT NULL DEFAULT 'ABIERTA',
  closed_at timestamptz,
  closed_by_doctor_id uuid REFERENCES public.doctors(id) ON DELETE SET NULL,
  closure_summary text,
  closure_signature_url text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 8. TABLA: public.anamnesis
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.anamnesis (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id uuid NOT NULL UNIQUE REFERENCES public.patients(id) ON DELETE CASCADE,
  tratamiento_medico_actual boolean NOT NULL DEFAULT false,
  toma_medicamentos boolean NOT NULL DEFAULT false,
  alergias boolean NOT NULL DEFAULT false,
  cardiopatias boolean NOT NULL DEFAULT false,
  alteracion_presion_arterial boolean NOT NULL DEFAULT false,
  embarazo boolean NOT NULL DEFAULT false,
  diabetes boolean NOT NULL DEFAULT false,
  hepatitis boolean NOT NULL DEFAULT false,
  irradiaciones boolean NOT NULL DEFAULT false,
  discrasias_sanguineas boolean NOT NULL DEFAULT false,
  fiebre_reumatica boolean NOT NULL DEFAULT false,
  enfermedades_renales boolean NOT NULL DEFAULT false,
  inmunosupresion boolean NOT NULL DEFAULT false,
  trastornos_emocionales boolean NOT NULL DEFAULT false,
  patologia_respiratoria boolean NOT NULL DEFAULT false,
  trastornos_gastricos boolean NOT NULL DEFAULT false,
  epilepsia boolean NOT NULL DEFAULT false,
  cirugias boolean NOT NULL DEFAULT false,
  enfermedades_orales boolean NOT NULL DEFAULT false,
  otras_alteraciones boolean NOT NULL DEFAULT false,
  observaciones text NOT NULL DEFAULT '',
  antecedentes_familiares text NOT NULL DEFAULT '',
  medicamentos_actuales text,
  alergias_descripcion text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 9. TABLA: public.evolutions
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.evolutions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  record_id uuid NOT NULL REFERENCES public.clinical_records(id) ON DELETE CASCADE,
  doctor_id uuid REFERENCES public.doctors(id) ON DELETE SET NULL,
  date date NOT NULL DEFAULT CURRENT_DATE,
  procedure_time time NOT NULL DEFAULT CURRENT_TIME,
  description text NOT NULL,
  doctor_name text,
  tooth_treated integer,
  cost numeric NOT NULL DEFAULT 0.0,
  payment_amount numeric NOT NULL DEFAULT 0.0,
  balance numeric NOT NULL DEFAULT 0.0,
  patient_signature_url text,
  doctor_signature_url text,
  voided_at timestamptz,
  voided_by text,
  void_reason text,
  amends_evolution_id uuid REFERENCES public.evolutions(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 10. TABLA: public.tooth_states
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.tooth_states (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  evolution_id uuid NOT NULL REFERENCES public.evolutions(id) ON DELETE CASCADE,
  tooth_number integer NOT NULL,
  surface text NOT NULL,
  condition text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 11. TABLA: public.periodontal_examinations
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.periodontal_examinations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  record_id uuid NOT NULL REFERENCES public.clinical_records(id) ON DELETE CASCADE,
  doctor_id uuid NOT NULL REFERENCES public.doctors(id) ON DELETE CASCADE,
  examination_date timestamptz NOT NULL DEFAULT now(),
  general_observations text,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 12. TABLA: public.periodontal_sites
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.periodontal_sites (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  examination_id uuid NOT NULL REFERENCES public.periodontal_examinations(id) ON DELETE CASCADE,
  tooth_number integer NOT NULL,
  site_location text NOT NULL,
  pocket_depth_mm real NOT NULL DEFAULT 1.0,
  gingival_recession_mm real NOT NULL DEFAULT 0.0,
  clinical_attachment_loss_mm real,
  bleeding_on_probing boolean NOT NULL DEFAULT false,
  suppuration boolean NOT NULL DEFAULT false,
  mobility_grade integer NOT NULL DEFAULT 0,
  furcation_grade integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ux_periodontal_sites UNIQUE (examination_id, tooth_number, site_location)
);

-- ============================================================================
-- 13. TABLA: public.informed_consents
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.informed_consents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id uuid NOT NULL REFERENCES public.patients(id) ON DELETE CASCADE,
  doctor_id uuid NOT NULL REFERENCES public.doctors(id) ON DELETE CASCADE,
  document_version text NOT NULL DEFAULT '2',
  version_date text NOT NULL DEFAULT '2019-06-01',
  treatment_summary text NOT NULL,
  representative_name text,
  representative_id text,
  patient_signature_url text,
  guardian_signature_url text,
  doctor_signature_url text,
  doctor_license_number text,
  signed_at timestamptz,
  status text NOT NULL DEFAULT 'SIGNED',
  revoked boolean NOT NULL DEFAULT false,
  revoked_at timestamptz,
  revocation_reason text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 14. TABLA: public.informed_consent_procedures
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.informed_consent_procedures (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  consent_id uuid NOT NULL REFERENCES public.informed_consents(id) ON DELETE CASCADE,
  tooth_number integer NOT NULL,
  procedure_date text NOT NULL,
  patient_signature_url text,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 15. TABLA: public.clinical_audit_log
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.clinical_audit_log (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id uuid NOT NULL REFERENCES public.patients(id) ON DELETE CASCADE,
  entity_table text NOT NULL,
  entity_id text NOT NULL,
  action text NOT NULL,
  changed_fields jsonb NOT NULL DEFAULT '[]'::jsonb,
  actor_user_id uuid,
  actor_name text,
  occurred_at timestamptz NOT NULL DEFAULT now()
);

-- Vista de compatibilidad para queries que referencien clinical_audit_logs en plural
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_views WHERE viewname = 'clinical_audit_logs') THEN
    CREATE VIEW public.clinical_audit_logs AS SELECT * FROM public.clinical_audit_log;
  END IF;
END $$;

-- ============================================================================
-- 16. TABLA: public.treatments
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.treatments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id uuid NOT NULL REFERENCES public.patients(id) ON DELETE CASCADE,
  description text NOT NULL DEFAULT '',
  total_cost numeric NOT NULL DEFAULT 0.0,
  status text NOT NULL DEFAULT 'PENDIENTE',
  status_reason text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- ============================================================================
-- 17. TABLA: public.payments
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.payments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  treatment_id uuid NOT NULL REFERENCES public.treatments(id) ON DELETE CASCADE,
  amount numeric NOT NULL DEFAULT 0.0,
  payment_method text DEFAULT 'EFECTIVO',
  created_at timestamptz NOT NULL DEFAULT now(),
  voided_at timestamptz,
  voided_by text,
  void_reason text
);

-- ============================================================================
-- ÍNDICES DE RENDIMIENTO E INTEGRIDAD
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_doctors_user_id ON public.doctors(user_id);
CREATE INDEX IF NOT EXISTS idx_patients_user_id ON public.patients(user_id);
CREATE INDEX IF NOT EXISTS idx_patients_dni ON public.patients(dni_cedula);
CREATE INDEX IF NOT EXISTS idx_patients_guardian ON public.patients(guardian_id);
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON public.appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON public.appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_start_time ON public.appointments(start_time);
CREATE INDEX IF NOT EXISTS idx_clinical_records_patient ON public.clinical_records(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_records_doctor ON public.clinical_records(closed_by_doctor_id);
CREATE INDEX IF NOT EXISTS idx_anamnesis_patient ON public.anamnesis(patient_id);
CREATE INDEX IF NOT EXISTS idx_evolutions_record ON public.evolutions(record_id);
CREATE INDEX IF NOT EXISTS idx_evolutions_doctor ON public.evolutions(doctor_id);
CREATE INDEX IF NOT EXISTS idx_tooth_states_evolution ON public.tooth_states(evolution_id);
CREATE INDEX IF NOT EXISTS idx_periodontal_exam_record ON public.periodontal_examinations(record_id);
CREATE INDEX IF NOT EXISTS idx_periodontal_exam_doctor ON public.periodontal_examinations(doctor_id);
CREATE INDEX IF NOT EXISTS idx_periodontal_sites_exam ON public.periodontal_sites(examination_id);
CREATE INDEX IF NOT EXISTS idx_informed_consents_patient ON public.informed_consents(patient_id);
CREATE INDEX IF NOT EXISTS idx_informed_consents_doctor ON public.informed_consents(doctor_id);
CREATE INDEX IF NOT EXISTS idx_informed_consent_proc_consent ON public.informed_consent_procedures(consent_id);
CREATE INDEX IF NOT EXISTS idx_clinical_audit_patient ON public.clinical_audit_log(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_audit_occurred ON public.clinical_audit_log(occurred_at);
CREATE INDEX IF NOT EXISTS idx_treatments_patient ON public.treatments(patient_id);
CREATE INDEX IF NOT EXISTS idx_payments_treatment ON public.payments(treatment_id);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

-- 1. DOCTORS
ALTER TABLE public.doctors ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Autenticados pueden ver doctores" ON public.doctors;
CREATE POLICY "Autenticados pueden ver doctores"
  ON public.doctors FOR SELECT
  TO authenticated
  USING (true);

DROP POLICY IF EXISTS "Doctores pueden actualizar su propio perfil" ON public.doctors;
CREATE POLICY "Doctores pueden actualizar su propio perfil"
  ON public.doctors FOR UPDATE
  TO authenticated
  USING (user_id = auth.uid() OR public.is_doctor())
  WITH CHECK (user_id = auth.uid() OR public.is_doctor());

DROP POLICY IF EXISTS "Doctores pueden insertar su propio perfil" ON public.doctors;
CREATE POLICY "Doctores pueden insertar su propio perfil"
  ON public.doctors FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid() OR public.is_doctor());

-- 2. GUARDIANS
ALTER TABLE public.guardians ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan acudientes" ON public.guardians;
CREATE POLICY "Doctores gestionan acudientes"
  ON public.guardians FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes pueden ver su acudiente" ON public.guardians;
CREATE POLICY "Pacientes pueden ver su acudiente"
  ON public.guardians FOR SELECT
  TO authenticated
  USING (id IN (SELECT guardian_id FROM public.patients WHERE user_id = auth.uid()));

-- 3. PATIENTS
ALTER TABLE public.patients ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan pacientes" ON public.patients;
CREATE POLICY "Doctores gestionan pacientes"
  ON public.patients FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan y editan su propio registro" ON public.patients;
CREATE POLICY "Pacientes consultan y editan su propio registro"
  ON public.patients FOR ALL
  TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

-- 4. APPOINTMENTS
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan todas las citas" ON public.appointments;
CREATE POLICY "Doctores gestionan todas las citas"
  ON public.appointments FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan sus citas base" ON public.appointments;
CREATE POLICY "Pacientes consultan sus citas base"
  ON public.appointments FOR SELECT
  TO authenticated
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()));

DROP POLICY IF EXISTS "Pacientes pueden insertar sus propias citas" ON public.appointments;
CREATE POLICY "Pacientes pueden insertar sus propias citas"
  ON public.appointments FOR INSERT
  TO authenticated
  WITH CHECK (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()));

DROP POLICY IF EXISTS "Pacientes pueden actualizar sus propias citas" ON public.appointments;
CREATE POLICY "Pacientes pueden actualizar sus propias citas"
  ON public.appointments FOR UPDATE
  TO authenticated
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()))
  WITH CHECK (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()));

-- 5. CLINICAL RECORDS
ALTER TABLE public.clinical_records ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan historias clínicas" ON public.clinical_records;
CREATE POLICY "Doctores gestionan historias clínicas"
  ON public.clinical_records FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan su historia clínica" ON public.clinical_records;
CREATE POLICY "Pacientes consultan su historia clínica"
  ON public.clinical_records FOR SELECT
  TO authenticated
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()));

-- 6. ANAMNESIS
ALTER TABLE public.anamnesis ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan anamnesis" ON public.anamnesis;
CREATE POLICY "Doctores gestionan anamnesis"
  ON public.anamnesis FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan su propia anamnesis" ON public.anamnesis;
CREATE POLICY "Pacientes consultan su propia anamnesis"
  ON public.anamnesis FOR SELECT
  TO authenticated
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()));

-- 7. EVOLUTIONS
ALTER TABLE public.evolutions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan evoluciones" ON public.evolutions;
CREATE POLICY "Doctores gestionan evoluciones"
  ON public.evolutions FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan evoluciones de su historia" ON public.evolutions;
CREATE POLICY "Pacientes consultan evoluciones de su historia"
  ON public.evolutions FOR SELECT
  TO authenticated
  USING (record_id IN (
    SELECT cr.id FROM public.clinical_records cr
    JOIN public.patients p ON cr.patient_id = p.id
    WHERE p.user_id = auth.uid()
  ));

-- 8. TOOTH STATES
ALTER TABLE public.tooth_states ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan estados dentales" ON public.tooth_states;
CREATE POLICY "Doctores gestionan estados dentales"
  ON public.tooth_states FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan sus estados dentales" ON public.tooth_states;
CREATE POLICY "Pacientes consultan sus estados dentales"
  ON public.tooth_states FOR SELECT
  TO authenticated
  USING (evolution_id IN (
    SELECT e.id FROM public.evolutions e
    JOIN public.clinical_records cr ON e.record_id = cr.id
    JOIN public.patients p ON cr.patient_id = p.id
    WHERE p.user_id = auth.uid()
  ));

-- 9. PERIODONTAL EXAMINATIONS
ALTER TABLE public.periodontal_examinations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan exámenes periodontales" ON public.periodontal_examinations;
CREATE POLICY "Doctores gestionan exámenes periodontales"
  ON public.periodontal_examinations FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan exámenes periodontales" ON public.periodontal_examinations;
CREATE POLICY "Pacientes consultan exámenes periodontales"
  ON public.periodontal_examinations FOR SELECT
  TO authenticated
  USING (record_id IN (
    SELECT cr.id FROM public.clinical_records cr
    JOIN public.patients p ON cr.patient_id = p.id
    WHERE p.user_id = auth.uid()
  ));

-- 10. PERIODONTAL SITES
ALTER TABLE public.periodontal_sites ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan sitios periodontales" ON public.periodontal_sites;
CREATE POLICY "Doctores gestionan sitios periodontales"
  ON public.periodontal_sites FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan sitios periodontales" ON public.periodontal_sites;
CREATE POLICY "Pacientes consultan sitios periodontales"
  ON public.periodontal_sites FOR SELECT
  TO authenticated
  USING (examination_id IN (
    SELECT pe.id FROM public.periodontal_examinations pe
    JOIN public.clinical_records cr ON pe.record_id = cr.id
    JOIN public.patients p ON cr.patient_id = p.id
    WHERE p.user_id = auth.uid()
  ));

-- 11. INFORMED CONSENTS
ALTER TABLE public.informed_consents ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan consentimientos informados" ON public.informed_consents;
CREATE POLICY "Doctores gestionan consentimientos informados"
  ON public.informed_consents FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan sus consentimientos" ON public.informed_consents;
CREATE POLICY "Pacientes consultan sus consentimientos"
  ON public.informed_consents FOR SELECT
  TO authenticated
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()));

-- 12. INFORMED CONSENT PROCEDURES
ALTER TABLE public.informed_consent_procedures ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan procedimientos de consentimiento" ON public.informed_consent_procedures;
CREATE POLICY "Doctores gestionan procedimientos de consentimiento"
  ON public.informed_consent_procedures FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan procedimientos de su consentimiento" ON public.informed_consent_procedures;
CREATE POLICY "Pacientes consultan procedimientos de su consentimiento"
  ON public.informed_consent_procedures FOR SELECT
  TO authenticated
  USING (consent_id IN (
    SELECT ic.id FROM public.informed_consents ic
    JOIN public.patients p ON ic.patient_id = p.id
    WHERE p.user_id = auth.uid()
  ));

-- 13. CLINICAL AUDIT LOG
ALTER TABLE public.clinical_audit_log ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores consultan e insertan auditoría" ON public.clinical_audit_log;
CREATE POLICY "Doctores consultan e insertan auditoría"
  ON public.clinical_audit_log FOR ALL
  TO authenticated
  USING (public.is_doctor())
  WITH CHECK (public.is_doctor());

DROP POLICY IF EXISTS "Pacientes consultan su propia auditoría clínica" ON public.clinical_audit_log;
CREATE POLICY "Pacientes consultan su propia auditoría clínica"
  ON public.clinical_audit_log FOR SELECT
  TO authenticated
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()));

-- 14. TREATMENTS
ALTER TABLE public.treatments ENABLE ROW LEVEL SECURITY;

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
  USING (patient_id IN (SELECT id FROM public.patients WHERE user_id = auth.uid()));

-- 15. PAYMENTS
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;

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
    WHERE p.user_id = auth.uid()
  ));
