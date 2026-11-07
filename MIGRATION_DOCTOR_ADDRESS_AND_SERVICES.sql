-- ==============================================================================
-- BIODONTOS: MIGRACIÓN DE COMPATIBILIDAD - DIRECCIÓN Y SERVICIOS EN DOCTORS
-- Fecha: 2026-11-01
-- Descripción: Agrega de forma segura e idempotente las columnas clinic_address y services
-- a la tabla public.doctors, asigna valores por defecto y recarga el schema cache de PostgREST.
-- ==============================================================================

-- 1. Agregar columnas si no existen
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name = 'doctors'
    ) THEN
        ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS clinic_address text;
        ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS services text;

        COMMENT ON COLUMN public.doctors.clinic_address IS 'Dirección física o sede del consultorio del especialista';
        COMMENT ON COLUMN public.doctors.services IS 'Lista de servicios clínicos y procedimientos ofrecidos, separados por comas';

        -- 2. Asignar valores por defecto a los doctores existentes que no tengan dirección o servicios
        UPDATE public.doctors
        SET clinic_address = 'Calle 100 # 15-20, Consultorio 402, Bogotá'
        WHERE clinic_address IS NULL OR trim(clinic_address) = '';

        UPDATE public.doctors
        SET services = 'Consulta Odontológica General, Limpieza y Profilaxis, Blanqueamiento Dental, Valoración de Urgencias'
        WHERE services IS NULL OR trim(services) = '';
    END IF;
END $$;

-- 3. Notificar a PostgREST para recargar el schema cache inmediatamente
NOTIFY pgrst, 'reload schema';
