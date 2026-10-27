-- ==============================================================================
-- BIODONTOS: MIGRACIÓN DE COMPATIBILIDAD - COLUMNAS BIO Y PHOTO_URL EN DOCTORS
-- Fecha: 2026-10-27
-- Descripción: Agrega de forma segura e idempotente las columnas bio y photo_url
-- a la tabla public.doctors, asegurando además la recarga del schema cache de PostgREST
-- y la correcta política RLS para actualización de perfil.
-- ==============================================================================

-- 1. Agregar columnas si no existen
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name = 'doctors'
    ) THEN
        ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS bio text;
        ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS photo_url text;

        COMMENT ON COLUMN public.doctors.bio IS 'Biografía y trayectoria profesional del especialista para perfil clínico y portal de pacientes';
        COMMENT ON COLUMN public.doctors.photo_url IS 'URL pública de la foto de perfil del especialista';
    END IF;
END $$;

-- 2. Asegurar que las políticas RLS permitan al doctor actualizar su propia información
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name = 'doctors'
    ) THEN
        -- Garantizar RLS habilitado
        ALTER TABLE public.doctors ENABLE ROW LEVEL SECURITY;

        -- Política para actualización de perfil propio por el doctor autenticado
        DROP POLICY IF EXISTS "Doctores actualizan su propio perfil" ON public.doctors;
        CREATE POLICY "Doctores actualizan su propio perfil"
            ON public.doctors
            FOR UPDATE
            TO authenticated
            USING (
                user_id::text = auth.jwt() ->> 'sub' 
                OR id::text = auth.jwt() ->> 'sub'
            )
            WITH CHECK (
                user_id::text = auth.jwt() ->> 'sub' 
                OR id::text = auth.jwt() ->> 'sub'
            );

        -- Política para consulta pública/autenticada de especialistas
        DROP POLICY IF EXISTS "Consulta de doctores para usuarios autenticados" ON public.doctors;
        CREATE POLICY "Consulta de doctores para usuarios autenticados"
            ON public.doctors
            FOR SELECT
            TO authenticated
            USING (true);
    END IF;
END $$;

-- 3. Notificar a PostgREST para recargar el schema cache inmediatamente
NOTIFY pgrst, 'reload schema';
