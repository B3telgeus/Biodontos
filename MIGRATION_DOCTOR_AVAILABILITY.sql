-- ============================================================================
-- BIODONTOS: MIGRACIÓN DE TABLA DOCTOR_AVAILABILITY (HORARIOS SEMANALES)
-- Ejecutar este script en el SQL Editor del dashboard de Supabase para solucionar:
-- "Could not find the table public.doctor_availability in the schema"
-- ============================================================================

-- 1. Extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Asegurar columnas bio y photo_url en public.doctors si existen
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'doctors') THEN
        ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS bio text;
        ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS photo_url text;
    END IF;
END $$;

-- 3. Crear tabla public.doctor_availability de forma resiliente
CREATE TABLE IF NOT EXISTS public.doctor_availability (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id text NOT NULL, -- Compatible con identificadores UUID y string
    day_of_week smallint NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1 = Lunes, 7 = Domingo (ISO-8601)
    start_time time NOT NULL,
    end_time time NOT NULL CHECK (end_time > start_time),
    slot_minutes smallint NOT NULL DEFAULT 30 CHECK (slot_minutes BETWEEN 10 AND 240),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone DEFAULT now()
);

-- Si doctors.id es tipo UUID, intentar agregar constraint si no existe
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'doctors' AND column_name = 'id' AND udt_name = 'uuid'
    ) THEN
        BEGIN
            ALTER TABLE public.doctor_availability 
                ALTER COLUMN doctor_id TYPE uuid USING (doctor_id::uuid);
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.table_constraints 
                WHERE constraint_name = 'fk_doctor_availability_doctor'
            ) THEN
                ALTER TABLE public.doctor_availability
                    ADD CONSTRAINT fk_doctor_availability_doctor
                    FOREIGN KEY (doctor_id) REFERENCES public.doctors(id) ON DELETE CASCADE;
            END IF;
        EXCEPTION WHEN OTHERS THEN
            -- Continúa en modo compatible si hay registros incompatibles
            NULL;
        END;
    END IF;
END $$;

-- 4. Índice único para evitar duplicar el mismo día para un doctor
CREATE UNIQUE INDEX IF NOT EXISTS ux_doctor_availability_day
    ON public.doctor_availability (doctor_id, day_of_week);

-- 5. Habilitar Row Level Security (RLS)
ALTER TABLE public.doctor_availability ENABLE ROW LEVEL SECURITY;

-- 6. Políticas de RLS
DROP POLICY IF EXISTS "Permitir lectura de disponibilidad a autenticados" ON public.doctor_availability;
CREATE POLICY "Permitir lectura de disponibilidad a autenticados"
    ON public.doctor_availability FOR SELECT
    TO authenticated
    USING (true);

DROP POLICY IF EXISTS "Doctores gestionan su disponibilidad" ON public.doctor_availability;
CREATE POLICY "Doctores gestionan su disponibilidad"
    ON public.doctor_availability FOR ALL
    TO authenticated
    USING (
        doctor_id::text IN (
            SELECT id::text FROM public.doctors WHERE user_id::text = auth.jwt() ->> 'sub'
        )
        OR (COALESCE(auth.jwt() -> 'user_metadata' ->> 'role', '') = 'DOCTOR')
        OR (COALESCE(auth.jwt() ->> 'role', '') = 'service_role')
    )
    WITH CHECK (
        doctor_id::text IN (
            SELECT id::text FROM public.doctors WHERE user_id::text = auth.jwt() ->> 'sub'
        )
        OR (COALESCE(auth.jwt() -> 'user_metadata' ->> 'role', '') = 'DOCTOR')
        OR (COALESCE(auth.jwt() ->> 'role', '') = 'service_role')
    );

-- 7. Insertar horarios por defecto para doctores existentes que aún no tengan disponibilidad
DO $$
DECLARE
    doc RECORD;
    d INT;
BEGIN
    FOR doc IN SELECT id::text AS doc_id FROM public.doctors LOOP
        IF NOT EXISTS (SELECT 1 FROM public.doctor_availability WHERE doctor_id::text = doc.doc_id) THEN
            -- Lunes (1) a Viernes (5) de 08:00 a 17:00, slots de 30 min
            FOR d IN 1..5 LOOP
                INSERT INTO public.doctor_availability (doctor_id, day_of_week, start_time, end_time, slot_minutes, is_active)
                VALUES (doc.doc_id, d, '08:00:00', '17:00:00', 30, true)
                ON CONFLICT (doctor_id, day_of_week) DO NOTHING;
            END LOOP;
            -- Sábado (6) y Domingo (7) inactivos por defecto
            INSERT INTO public.doctor_availability (doctor_id, day_of_week, start_time, end_time, slot_minutes, is_active)
            VALUES (doc.doc_id, 6, '08:00:00', '13:00:00', 30, false)
            ON CONFLICT (doctor_id, day_of_week) DO NOTHING;
            INSERT INTO public.doctor_availability (doctor_id, day_of_week, start_time, end_time, slot_minutes, is_active)
            VALUES (doc.doc_id, 7, '08:00:00', '12:00:00', 30, false)
            ON CONFLICT (doctor_id, day_of_week) DO NOTHING;
        END IF;
    END LOOP;
END $$;

-- 8. Forzar recarga inmediata de la caché de PostgREST
NOTIFY pgrst, 'reload schema';
