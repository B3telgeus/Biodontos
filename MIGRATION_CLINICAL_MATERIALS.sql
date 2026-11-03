-- =============================================================================
-- MIGRACIÓN: MÓDULO DE MATERIALES CLÍNICOS (INVIMA) Y TRAZABILIDAD EN EVOLUCIONES
-- Proyecto: Biodontos
-- Archivo: MIGRATION_CLINICAL_MATERIALS.sql
-- Cumplimiento: Resolución 1995 de 1999 (Inmutabilidad y Trazabilidad Médica)
-- Motor: PostgreSQL 15+ / Supabase
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. FUNCIONES AUXILIARES: SEGURIDAD Y RESOLUCIÓN DE DOCTOR AUTENTICADO
-- -----------------------------------------------------------------------------

-- Función de validación de rol de Doctor (idempotente y resiliente)
CREATE OR REPLACE FUNCTION public.is_doctor()
RETURNS boolean AS $$
BEGIN
  RETURN (
    COALESCE(auth.jwt() -> 'user_metadata' ->> 'role', '') = 'DOCTOR'
    OR COALESCE(auth.jwt() ->> 'role', '') = 'service_role'
    OR EXISTS (
      SELECT 1 FROM public.doctors 
      WHERE user_id::text = auth.jwt() ->> 'sub' 
         OR id::text = auth.jwt() ->> 'sub'
         OR user_id = auth.uid()
         OR id = auth.uid()
    )
  );
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.is_doctor() TO authenticated, service_role;

-- Función para resolver el doctor_id del usuario autenticado actual
CREATE OR REPLACE FUNCTION public.get_current_doctor_id()
RETURNS uuid
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT id FROM public.doctors 
  WHERE user_id = auth.uid() OR id = auth.uid() 
  LIMIT 1;
$$;

GRANT EXECUTE ON FUNCTION public.get_current_doctor_id() TO authenticated, service_role;

-- -----------------------------------------------------------------------------
-- 2. TABLA: CLINICAL_MATERIALS (Catálogo de Materiales por Doctor)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.clinical_materials (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  doctor_id uuid NOT NULL DEFAULT public.get_current_doctor_id(),
  name text NOT NULL,
  invima_registration text NOT NULL,
  batch_lot_number text NOT NULL,
  manufacturing_date date NOT NULL,
  expiration_date date NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT clinical_materials_pkey PRIMARY KEY (id),
  CONSTRAINT clinical_materials_doctor_id_fkey FOREIGN KEY (doctor_id) 
    REFERENCES public.doctors(id) ON DELETE CASCADE
);

-- Retrocompatibilidad: Si la tabla ya existía (ej. bosquejo en SQL0710.txt), ajustar columnas
ALTER TABLE public.clinical_materials
  ADD COLUMN IF NOT EXISTS doctor_id uuid REFERENCES public.doctors(id) ON DELETE CASCADE,
  ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS created_at timestamp with time zone NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS updated_at timestamp with time zone NOT NULL DEFAULT now(),
  DROP COLUMN IF EXISTS current_stock,
  DROP COLUMN IF EXISTS unit_of_measure;

-- Convertir columnas de texto a tipo text para evitar truncamientos
ALTER TABLE public.clinical_materials
  ALTER COLUMN name TYPE text,
  ALTER COLUMN invima_registration TYPE text,
  ALTER COLUMN batch_lot_number TYPE text;

-- Si existieran registros legados con doctor_id nulo, asociarlos al primer doctor registrado
UPDATE public.clinical_materials
SET doctor_id = (SELECT id FROM public.doctors ORDER BY created_at ASC LIMIT 1)
WHERE doctor_id IS NULL;

-- Asignar DEFAULT y NOT NULL a doctor_id
ALTER TABLE public.clinical_materials
  ALTER COLUMN doctor_id SET DEFAULT public.get_current_doctor_id();

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM public.clinical_materials WHERE doctor_id IS NULL) THEN
    ALTER TABLE public.clinical_materials ALTER COLUMN doctor_id SET NOT NULL;
  END IF;
END $$;

-- Asegurar restricción de llave foránea hacia doctors
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conname = 'clinical_materials_doctor_id_fkey' 
      AND conrelid = 'public.clinical_materials'::regclass
  ) THEN
    ALTER TABLE public.clinical_materials 
      ADD CONSTRAINT clinical_materials_doctor_id_fkey 
      FOREIGN KEY (doctor_id) REFERENCES public.doctors(id) ON DELETE CASCADE;
  END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 3. RESTRICCIONES DE INTEGRIDAD: CLINICAL_MATERIALS
-- -----------------------------------------------------------------------------
DO $$
BEGIN
  -- Limpiar versiones previas de la restricción de fechas si existieran
  IF EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conname = 'chk_clinical_materials_dates' 
      AND conrelid = 'public.clinical_materials'::regclass
  ) THEN
    ALTER TABLE public.clinical_materials DROP CONSTRAINT chk_clinical_materials_dates;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conname = 'chk_clinical_materials_expiration_after_mfg' 
      AND conrelid = 'public.clinical_materials'::regclass
  ) THEN
    ALTER TABLE public.clinical_materials 
      ADD CONSTRAINT chk_clinical_materials_expiration_after_mfg 
      CHECK (expiration_date > manufacturing_date);
  END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 4. ÍNDICES: CLINICAL_MATERIALS
-- -----------------------------------------------------------------------------
-- Deduplicación idempotente previa a la creación del índice único parcial
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes 
    WHERE schemaname = 'public' 
      AND tablename = 'clinical_materials' 
      AND indexname = 'ux_clinical_materials_doctor_invima_lot_active'
  ) THEN
    -- Desactivar duplicados antiguos dejando el más reciente activo
    WITH ranked_duplicates AS (
      SELECT id,
             row_number() OVER (
               PARTITION BY doctor_id, upper(trim(invima_registration)), upper(trim(batch_lot_number))
               ORDER BY created_at DESC, id DESC
             ) as rn
      FROM public.clinical_materials
      WHERE is_active = true
    )
    UPDATE public.clinical_materials
    SET is_active = false
    WHERE id IN (SELECT id FROM ranked_duplicates WHERE rn > 1);

    CREATE UNIQUE INDEX ux_clinical_materials_doctor_invima_lot_active
      ON public.clinical_materials (doctor_id, upper(trim(invima_registration)), upper(trim(batch_lot_number)))
      WHERE is_active = true;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_clinical_materials_doctor_active 
  ON public.clinical_materials (doctor_id, is_active, expiration_date);

-- -----------------------------------------------------------------------------
-- 5. TABLA: EVOLUTION_MATERIALS_USED (Snapshots Inmutables en Evoluciones)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.evolution_materials_used (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  evolution_id uuid NOT NULL,
  material_id uuid,
  quantity_used numeric(6, 2) NOT NULL DEFAULT 1.0,
  material_name_snapshot text NOT NULL,
  invima_snapshot text NOT NULL,
  lot_number_snapshot text NOT NULL,
  manufacturing_date_snapshot date NOT NULL,
  expiration_date_snapshot date NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT evolution_materials_used_pkey PRIMARY KEY (id),
  CONSTRAINT evolution_materials_used_evolution_id_fkey FOREIGN KEY (evolution_id) 
    REFERENCES public.evolutions(id) ON DELETE CASCADE,
  CONSTRAINT evolution_materials_used_material_id_fkey FOREIGN KEY (material_id) 
    REFERENCES public.clinical_materials(id) ON DELETE SET NULL
);

-- Retrocompatibilidad: Añadir columnas snapshot si la tabla ya existía
ALTER TABLE public.evolution_materials_used
  ADD COLUMN IF NOT EXISTS material_name_snapshot text,
  ADD COLUMN IF NOT EXISTS manufacturing_date_snapshot date,
  ADD COLUMN IF NOT EXISTS quantity_used numeric(6, 2) NOT NULL DEFAULT 1.0,
  ADD COLUMN IF NOT EXISTS created_at timestamp with time zone NOT NULL DEFAULT now();

-- Normalizar columnas de texto a tipo text
ALTER TABLE public.evolution_materials_used
  ALTER COLUMN lot_number_snapshot TYPE text,
  ALTER COLUMN invima_snapshot TYPE text;

-- Backfill de snapshots para registros históricos preexistentes
UPDATE public.evolution_materials_used emu
SET material_name_snapshot = COALESCE(cm.name, 'Material Clínico'),
    manufacturing_date_snapshot = COALESCE(cm.manufacturing_date, '2020-01-01'::date)
FROM public.clinical_materials cm
WHERE emu.material_id = cm.id
  AND (emu.material_name_snapshot IS NULL OR emu.manufacturing_date_snapshot IS NULL);

UPDATE public.evolution_materials_used
SET material_name_snapshot = 'Material Clínico'
WHERE material_name_snapshot IS NULL;

UPDATE public.evolution_materials_used
SET manufacturing_date_snapshot = '2020-01-01'::date
WHERE manufacturing_date_snapshot IS NULL;

-- Asignar NOT NULL a las columnas completadas
ALTER TABLE public.evolution_materials_used
  ALTER COLUMN material_name_snapshot SET NOT NULL,
  ALTER COLUMN manufacturing_date_snapshot SET NOT NULL;

-- Restricción de cantidad positiva
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint 
    WHERE conname = 'chk_evolution_materials_quantity' 
      AND conrelid = 'public.evolution_materials_used'::regclass
  ) THEN
    ALTER TABLE public.evolution_materials_used 
      ADD CONSTRAINT chk_evolution_materials_quantity 
      CHECK (quantity_used > 0);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_evolution_materials_evo 
  ON public.evolution_materials_used (evolution_id);

CREATE INDEX IF NOT EXISTS idx_evolution_materials_material 
  ON public.evolution_materials_used (material_id);

-- -----------------------------------------------------------------------------
-- 6. TRIGGERS: INTEGRIDAD CLÍNICA, INMUTABILIDAD Y CAPTURA DE SNAPSHOTS
-- -----------------------------------------------------------------------------

-- 6.1. Abortar eliminación física en clinical_materials (forzar borrado lógico)
CREATE OR REPLACE FUNCTION public.prevent_clinical_material_physical_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  RAISE EXCEPTION 'Operación denegada por normativa clínica y trazabilidad INVIMA (Res. 1995/1999): No se permite la eliminación física de materiales clínicos. Utilice la desactivación lógica (is_active = false).';
  RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_delete_clinical_materials ON public.clinical_materials;
CREATE TRIGGER trg_prevent_delete_clinical_materials
  BEFORE DELETE ON public.clinical_materials
  FOR EACH ROW EXECUTE FUNCTION public.prevent_clinical_material_physical_delete();

-- 6.2. Auto-asignación de doctor_id, validación de fechas y timestamp en clinical_materials
CREATE OR REPLACE FUNCTION public.trg_clinical_materials_before_write()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  -- Auto-asignar doctor_id si no fue provisto
  IF NEW.doctor_id IS NULL THEN
    NEW.doctor_id := public.get_current_doctor_id();
  END IF;

  -- Fallback defensivo para operaciones ejecutadas por service_role
  IF NEW.doctor_id IS NULL THEN
    SELECT id INTO NEW.doctor_id FROM public.doctors ORDER BY created_at ASC LIMIT 1;
  END IF;

  -- Sanitización de cadenas
  NEW.name := TRIM(NEW.name);
  NEW.invima_registration := TRIM(NEW.invima_registration);
  NEW.batch_lot_number := TRIM(NEW.batch_lot_number);

  -- Validación estricta de orden de fechas
  IF NEW.expiration_date <= NEW.manufacturing_date THEN
    RAISE EXCEPTION 'La fecha de vencimiento (%) debe ser estrictamente posterior a la fecha de elaboración (%).',
      NEW.expiration_date, NEW.manufacturing_date;
  END IF;

  -- Actualizar marca temporal
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

-- Alias de función para retrocompatibilidad
CREATE OR REPLACE FUNCTION public.trg_before_clinical_material_upsert()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  RETURN public.trg_clinical_materials_before_write();
END;
$$;

DROP TRIGGER IF EXISTS trg_before_upsert_clinical_materials ON public.clinical_materials;
DROP TRIGGER IF EXISTS trg_clinical_materials_before_write ON public.clinical_materials;
CREATE TRIGGER trg_clinical_materials_before_write
  BEFORE INSERT OR UPDATE ON public.clinical_materials
  FOR EACH ROW EXECUTE FUNCTION public.trg_clinical_materials_before_write();

-- 6.3. Procesar snapshot y validar vigencia en evolution_materials_used
CREATE OR REPLACE FUNCTION public.trg_process_evolution_material_used()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_material record;
  v_evolution record;
  v_evo_date date;
BEGIN
  -- 1. Validar existencia de la evolución médica vinculada
  SELECT * INTO v_evolution
  FROM public.evolutions
  WHERE id = NEW.evolution_id;

  IF v_evolution.id IS NULL THEN
    RAISE EXCEPTION 'La evolución médica con id % no existe.', NEW.evolution_id;
  END IF;

  -- 2. Validar que la evolución no esté anulada (voided)
  IF v_evolution.voided_at IS NOT NULL THEN
    RAISE EXCEPTION 'Operación clínica denegada: La evolución médica ya fue anulada y no admite nuevos insumos.';
  END IF;

  -- 3. Si viene material_id, validar contra catálogo activo y vigente
  IF NEW.material_id IS NOT NULL THEN
    SELECT * INTO v_material
    FROM public.clinical_materials
    WHERE id = NEW.material_id;

    IF v_material.id IS NULL THEN
      RAISE EXCEPTION 'El material clínico con id % no existe en el catálogo.', NEW.material_id;
    END IF;

    -- Validación: Insumo ACTIVO
    IF NOT v_material.is_active THEN
      RAISE EXCEPTION 'Operación clínica denegada: El material "%" (Lote %) está inactivo en el catálogo y no puede ser utilizado.',
        v_material.name, v_material.batch_lot_number;
    END IF;

    -- Validación: Insumo NO VENCIDO (contra fecha de evolución y contra fecha actual)
    v_evo_date := COALESCE(v_evolution.date::date, v_evolution.created_at::date, CURRENT_DATE);

    IF v_material.expiration_date < v_evo_date OR v_material.expiration_date < CURRENT_DATE THEN
      RAISE EXCEPTION 'Operación clínica denegada: El material "%" (Lote %) se encuentra vencido (Vencimiento: %; Atención: %) y no puede suministrarse al paciente.',
        v_material.name, v_material.batch_lot_number, v_material.expiration_date, v_evo_date;
    END IF;

    -- Capturar snapshots automáticamente desde el material si el cliente no los envió
    NEW.material_name_snapshot := COALESCE(NULLIF(TRIM(NEW.material_name_snapshot), ''), v_material.name);
    NEW.invima_snapshot := COALESCE(NULLIF(TRIM(NEW.invima_snapshot), ''), v_material.invima_registration);
    NEW.lot_number_snapshot := COALESCE(NULLIF(TRIM(NEW.lot_number_snapshot), ''), v_material.batch_lot_number);
    NEW.manufacturing_date_snapshot := COALESCE(NEW.manufacturing_date_snapshot, v_material.manufacturing_date);
    NEW.expiration_date_snapshot := COALESCE(NEW.expiration_date_snapshot, v_material.expiration_date);
  END IF;

  -- 4. Validar cantidad decimal positiva
  IF NEW.quantity_used IS NULL OR NEW.quantity_used <= 0 THEN
    NEW.quantity_used := 1.0;
  END IF;

  -- 5. Validar integridad estricta del snapshot
  IF NEW.material_name_snapshot IS NULL OR TRIM(NEW.material_name_snapshot) = '' THEN
    RAISE EXCEPTION 'El nombre del material clínico en el snapshot no puede estar vacío.';
  END IF;
  IF NEW.invima_snapshot IS NULL OR TRIM(NEW.invima_snapshot) = '' THEN
    RAISE EXCEPTION 'El registro INVIMA en el snapshot no puede estar vacío.';
  END IF;
  IF NEW.lot_number_snapshot IS NULL OR TRIM(NEW.lot_number_snapshot) = '' THEN
    RAISE EXCEPTION 'El número de lote en el snapshot no puede estar vacío.';
  END IF;
  IF NEW.manufacturing_date_snapshot IS NULL THEN
    RAISE EXCEPTION 'La fecha de elaboración en el snapshot es obligatoria.';
  END IF;
  IF NEW.expiration_date_snapshot IS NULL THEN
    RAISE EXCEPTION 'La fecha de vencimiento en el snapshot es obligatoria.';
  END IF;
  IF NEW.expiration_date_snapshot <= NEW.manufacturing_date_snapshot THEN
    RAISE EXCEPTION 'La fecha de vencimiento del snapshot (%) debe ser posterior a la de elaboración (%).',
      NEW.expiration_date_snapshot, NEW.manufacturing_date_snapshot;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_snapshot_evolution_materials_used ON public.evolution_materials_used;
DROP TRIGGER IF EXISTS trg_process_evolution_material_used ON public.evolution_materials_used;
CREATE TRIGGER trg_process_evolution_material_used
  BEFORE INSERT ON public.evolution_materials_used
  FOR EACH ROW EXECUTE FUNCTION public.trg_process_evolution_material_used();

-- 6.4. Inmutabilidad estricta de insumos en evoluciones (bloquear UPDATE y DELETE)
CREATE OR REPLACE FUNCTION public.prevent_evolution_materials_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    RAISE EXCEPTION 'Operación clínica prohibida (Res. 1995 de 1999): Los insumos y medicamentos vinculados a una evolución son inmutables y no pueden eliminarse físicamente.';
  END IF;

  IF TG_OP = 'UPDATE' THEN
    -- Permitir si la actualización es un no-op (mismo contenido generado por un upsert idempotente)
    IF to_jsonb(OLD) = to_jsonb(NEW) THEN
      RETURN NEW;
    END IF;

    RAISE EXCEPTION 'Operación clínica prohibida (Res. 1995 de 1999): Los registros de insumos vinculados a una evolución médica son inmutables y no pueden modificarse tras su firma.';
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_immutable_evolution_materials ON public.evolution_materials_used;
DROP TRIGGER IF EXISTS trg_prevent_evolution_materials_mutation ON public.evolution_materials_used;
CREATE TRIGGER trg_prevent_evolution_materials_mutation
  BEFORE UPDATE OR DELETE ON public.evolution_materials_used
  FOR EACH ROW EXECUTE FUNCTION public.prevent_evolution_materials_mutation();

-- -----------------------------------------------------------------------------
-- 7. POLÍTICAS DE SEGURIDAD A NIVEL DE FILA (ROW LEVEL SECURITY - RLS)
-- -----------------------------------------------------------------------------

-- 7.1. RLS en clinical_materials: Aislamiento estricto por doctor
ALTER TABLE public.clinical_materials ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores gestionan inventario de materiales" ON public.clinical_materials;
DROP POLICY IF EXISTS "Doctores leen sus propios materiales" ON public.clinical_materials;
DROP POLICY IF EXISTS "Doctores insertan sus propios materiales" ON public.clinical_materials;
DROP POLICY IF EXISTS "Doctores actualizan sus propios materiales" ON public.clinical_materials;

CREATE POLICY "Doctores leen sus propios materiales"
  ON public.clinical_materials FOR SELECT
  TO authenticated
  USING (
    doctor_id IN (
      SELECT id FROM public.doctors WHERE user_id = auth.uid() OR id = auth.uid()
    )
  );

CREATE POLICY "Doctores insertan sus propios materiales"
  ON public.clinical_materials FOR INSERT
  TO authenticated
  WITH CHECK (
    doctor_id IN (
      SELECT id FROM public.doctors WHERE user_id = auth.uid() OR id = auth.uid()
    )
  );

CREATE POLICY "Doctores actualizan sus propios materiales"
  ON public.clinical_materials FOR UPDATE
  TO authenticated
  USING (
    doctor_id IN (
      SELECT id FROM public.doctors WHERE user_id = auth.uid() OR id = auth.uid()
    )
  )
  WITH CHECK (
    doctor_id IN (
      SELECT id FROM public.doctors WHERE user_id = auth.uid() OR id = auth.uid()
    )
  );

-- NOTA DE SEGURIDAD:
-- No se define política de DELETE en clinical_materials para usuarios autenticados.
-- En PostgreSQL RLS, la ausencia de política implica denegación por defecto (DENY ALL DELETE),
-- complementada por el trigger prevent_clinical_material_physical_delete().

-- 7.2. RLS en evolution_materials_used: Lectura para doctores y pacientes; inserción médica
ALTER TABLE public.evolution_materials_used ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Doctores registran materiales consumidos en evolucion" ON public.evolution_materials_used;
DROP POLICY IF EXISTS "Doctores leen materiales de evoluciones" ON public.evolution_materials_used;
DROP POLICY IF EXISTS "Pacientes leen materiales de sus evoluciones" ON public.evolution_materials_used;
DROP POLICY IF EXISTS "Doctores insertan materiales en evoluciones" ON public.evolution_materials_used;

CREATE POLICY "Doctores leen materiales de evoluciones"
  ON public.evolution_materials_used FOR SELECT
  TO authenticated
  USING (public.is_doctor());

CREATE POLICY "Pacientes leen materiales de sus evoluciones"
  ON public.evolution_materials_used FOR SELECT
  TO authenticated
  USING (
    evolution_id IN (
      SELECT e.id FROM public.evolutions e
      JOIN public.clinical_records cr ON e.record_id = cr.id
      WHERE cr.patient_id IN (
        SELECT id FROM public.patients WHERE user_id = auth.uid() OR id::text = auth.jwt() ->> 'sub'
      )
    )
  );

CREATE POLICY "Doctores insertan materiales en evoluciones"
  ON public.evolution_materials_used FOR INSERT
  TO authenticated
  WITH CHECK (public.is_doctor());

-- NOTA DE SEGURIDAD:
-- No se definen políticas de UPDATE ni DELETE en evolution_materials_used.
-- Toda mutación queda denegada por RLS y por el trigger prevent_evolution_materials_mutation().

-- -----------------------------------------------------------------------------
-- 8. NOTIFICACIÓN DE RECARGA DE ESQUEMA A POSTGREST (SUPABASE)
-- -----------------------------------------------------------------------------
NOTIFY pgrst, 'reload schema';
