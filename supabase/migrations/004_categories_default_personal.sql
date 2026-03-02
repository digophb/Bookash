-- Migration: Categorias Padrão + Personalizadas por Usuário
-- Description: Ajusta RLS policies para permitir categorias padrão (user_id NULL)
-- Author: TED (Backend Agent)
-- Date: 2026-03-02
-- Card: #10 - FEATURE - Categorias Padrão + Personalizadas

-- ============================================================================
-- STEP 1: Drop existing RLS policies for categories
-- ============================================================================

DROP POLICY IF EXISTS "Users can view own categories" ON public.categories;
DROP POLICY IF EXISTS "Users can insert own categories" ON public.categories;
DROP POLICY IF EXISTS "Users can update own categories" ON public.categories;
DROP POLICY IF EXISTS "Users can delete own categories" ON public.categories;

-- ============================================================================
-- STEP 2: Create new RLS policies with default category support
-- ============================================================================

-- SELECT: Users can view default categories (user_id NULL) + their own
CREATE POLICY "Users can view default and own categories" ON public.categories
    FOR SELECT USING (user_id IS NULL OR auth.uid() = user_id);

-- INSERT: Users can only insert their own categories (not default)
CREATE POLICY "Users can insert own categories" ON public.categories
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- UPDATE: Users can only update their own categories (not default)
CREATE POLICY "Users can update own categories" ON public.categories
    FOR UPDATE USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- DELETE: Users can only delete their own categories (not default)
CREATE POLICY "Users can delete own categories" ON public.categories
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================================
-- STEP 3: Add is_default flag for easier querying
-- ============================================================================

-- Add is_default column (generated from user_id)
ALTER TABLE public.categories 
ADD COLUMN IF NOT EXISTS is_default BOOLEAN GENERATED ALWAYS AS (user_id IS NULL) STORED;

-- Create index for is_default
CREATE INDEX IF NOT EXISTS idx_categories_is_default ON public.categories(is_default);

-- ============================================================================
-- STEP 4: Insert default categories if not exist
-- ============================================================================

-- Default income categories
INSERT INTO public.categories (name, type, color, icon, user_id)
VALUES 
    ('Salário', 'income', '#4CAF50', 'salary', NULL),
    ('Freelance', 'income', '#8BC34A', 'freelance', NULL),
    ('Investimentos', 'income', '#CDDC39', 'investment', NULL),
    ('Presente', 'income', '#FFC107', 'gift', NULL),
    ('Outros', 'income', '#FF9800', 'other', NULL)
ON CONFLICT DO NOTHING;

-- Default expense categories
INSERT INTO public.categories (name, type, color, icon, user_id)
VALUES 
    ('Alimentação', 'expense', '#F44336', 'restaurant', NULL),
    ('Transporte', 'expense', '#E91E63', 'car', NULL),
    ('Moradia', 'expense', '#9C27B0', 'home', NULL),
    ('Saúde', 'expense', '#673AB7', 'health', NULL),
    ('Educação', 'expense', '#3F51B5', 'education', NULL),
    ('Lazer', 'expense', '#2196F3', 'entertainment', NULL),
    ('Compras', 'expense', '#03A9F4', 'shopping', NULL),
    ('Serviços', 'expense', '#00BCD4', 'services', NULL),
    ('Outros', 'expense', '#009688', 'other', NULL)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- STEP 5: Comments for documentation
-- ============================================================================

COMMENT ON COLUMN public.categories.is_default IS 'TRUE = default category (user_id is NULL), FALSE = user-created category';
COMMENT ON TABLE public.categories IS 'Categories for transactions. Default categories (user_id NULL) are visible to all users. Users can create personal categories.';

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. Default categories have user_id = NULL and is_default = TRUE
-- 2. Personal categories have user_id set and is_default = FALSE
-- 3. Users CANNOT edit/delete default categories (RLS prevents this)
-- 4. Users CAN view default + their own categories
-- 5. The is_default column is GENERATED ALWAYS, so it's always in sync
