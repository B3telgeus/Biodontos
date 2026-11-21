-- ============================================================================
-- Biodontos - Migración: Columnas para Formularios de Consentimiento Clínico
-- Fecha: 2026-11-21
-- Soporte para: Odontología General, Blanqueamiento Dental y Rehabilitación Oral
-- ============================================================================

-- 1. EXTENDER TABLA: public.informed_consents
ALTER TABLE public.informed_consents
  ADD COLUMN IF NOT EXISTS consent_type text NOT NULL DEFAULT 'GENERAL_ADMISSION',
  ADD COLUMN IF NOT EXISTS patient_city text DEFAULT 'Bogotá D.C.',
  ADD COLUMN IF NOT EXISTS selected_procedures jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS acknowledged_risks jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS relevant_medical_conditions text,
  ADD COLUMN IF NOT EXISTS occlusion_test_accepted boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS aesthetic_color_accepted boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS initial_color_guide text,
  ADD COLUMN IF NOT EXISTS final_color_guide text,
  ADD COLUMN IF NOT EXISTS specific_notes text;

-- Restricción de valores válidos para consent_type
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_informed_consents_consent_type'
  ) THEN
    ALTER TABLE public.informed_consents
      ADD CONSTRAINT chk_informed_consents_consent_type
      CHECK (consent_type IN ('GENERAL_ADMISSION', 'BLEACHING', 'GENERAL_ODONTOLOGY', 'ORAL_REHABILITATION'));
  END IF;
END $$;

-- Comentarios explicativos en columnas de public.informed_consents
COMMENT ON COLUMN public.informed_consents.consent_type IS 'Tipo de formulario de consentimiento (GENERAL_ADMISSION, BLEACHING, GENERAL_ODONTOLOGY, ORAL_REHABILITATION)';
COMMENT ON COLUMN public.informed_consents.patient_city IS 'Ciudad donde se suscribe el consentimiento informado';
COMMENT ON COLUMN public.informed_consents.selected_procedures IS 'Lista JSON de identificadores o códigos de procedimientos autorizados';
COMMENT ON COLUMN public.informed_consents.acknowledged_risks IS 'Lista JSON de riesgos y complicaciones informadas aceptadas por el paciente';
COMMENT ON COLUMN public.informed_consents.relevant_medical_conditions IS 'Condiciones sistémicas o médicas relevantes informadas para el procedimiento';
COMMENT ON COLUMN public.informed_consents.occlusion_test_accepted IS 'Confirmación de prueba de ajuste oclusal aceptada (Rehabilitación Oral)';
COMMENT ON COLUMN public.informed_consents.aesthetic_color_accepted IS 'Confirmación de tono/color y forma estética aceptada (Rehabilitación Oral)';
COMMENT ON COLUMN public.informed_consents.initial_color_guide IS 'Guía de color dental inicial antes del tratamiento (Blanqueamiento / Estética)';
COMMENT ON COLUMN public.informed_consents.final_color_guide IS 'Guía de color dental objetivo o final registrado';
COMMENT ON COLUMN public.informed_consents.specific_notes IS 'Observaciones, alternativas explicadas o acuerdos específicos del procedimiento';

-- 2. EXTENDER TABLA: public.informed_consent_procedures
ALTER TABLE public.informed_consent_procedures
  ADD COLUMN IF NOT EXISTS procedure_name text,
  ADD COLUMN IF NOT EXISTS doctor_signature_url text;

-- Comentarios explicativos en columnas de public.informed_consent_procedures
COMMENT ON COLUMN public.informed_consent_procedures.procedure_name IS 'Nombre descriptivo del procedimiento específico realizado sobre la pieza dental';
COMMENT ON COLUMN public.informed_consent_procedures.doctor_signature_url IS 'URL de la firma digital del odontólogo responsable del procedimiento específico';

-- 3. ÍNDICES DE RENDIMIENTO
CREATE INDEX IF NOT EXISTS idx_informed_consents_patient_type
  ON public.informed_consents (patient_id, consent_type);

CREATE INDEX IF NOT EXISTS idx_informed_consents_type
  ON public.informed_consents (consent_type);
