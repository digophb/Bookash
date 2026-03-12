-- Migration: 022_fix_default_categories_trigger
-- Date: 2026-03-12
-- Description: Fix trigger that creates default categories for new users
--              and add transfer category

-- ============================================================================
-- Drop existing trigger and function
-- ============================================================================
DROP TRIGGER IF EXISTS on_user_created_categories ON auth.users;
DROP FUNCTION IF EXISTS public.create_default_categories();

-- ============================================================================
-- Create improved function with explicit UUID handling
-- ============================================================================
CREATE OR REPLACE FUNCTION public.create_default_categories()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = postgres
LANGUAGE plpgsql
AS $$
DECLARE
    v_user_uuid UUID;
BEGIN
    -- Ensure user_id is properly cast to UUID
    v_user_uuid := NEW.id::UUID;
    
    -- Insert default categories with explicit UUID
    INSERT INTO public.categories (id, user_id, name, type, color, icon)
    VALUES 
        (gen_random_uuid(), v_user_uuid, 'Alimentação', 'expense', '#FF6B6B', 'food'),
        (gen_random_uuid(), v_user_uuid, 'Transporte', 'expense', '#4ECDC4', 'transport'),
        (gen_random_uuid(), v_user_uuid, 'Salário', 'income', '#45B7D1', 'salary'),
        (gen_random_uuid(), v_user_uuid, 'Lazer', 'expense', '#96CEB4', 'entertainment'),
        (gen_random_uuid(), v_user_uuid, 'Saúde', 'expense', '#FFEAA7', 'health'),
        (gen_random_uuid(), v_user_uuid, 'Educação', 'expense', '#DDA0DD', 'education'),
        (gen_random_uuid(), v_user_uuid, 'Moradia', 'expense', '#98D8C8', 'home'),
        (gen_random_uuid(), v_user_uuid, 'Outros', 'expense', '#B0BEC5', 'other'),
        (gen_random_uuid(), v_user_uuid, 'Transferência', 'transfer', '#9B59B6', 'transfer');
    
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Erro ao criar categorias padrão: %', SQLERRM;
    RETURN NEW;
END;
$$;

-- ============================================================================
-- Recreate trigger
-- ============================================================================
CREATE TRIGGER on_user_created_categories
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.create_default_categories();

-- ============================================================================
-- Add transfer category to existing users
-- ============================================================================
INSERT INTO public.categories (id, user_id, name, type, color, icon)
SELECT 
    gen_random_uuid(),
    u.id,
    'Transferência',
    'transfer',
    '#9B59B6',
    'transfer'
FROM auth.users u
WHERE NOT EXISTS (
    SELECT 1 FROM public.categories c 
    WHERE c.user_id = u.id AND c.name = 'Transferência'
)
ON CONFLICT (user_id, name, type) DO NOTHING;

-- ============================================================================
-- Comments
-- ============================================================================
COMMENT ON FUNCTION public.create_default_categories() IS 'Cria categorias padrão para novos usuários incluindo categoria de transferência';
