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
BEGIN
    -- Insert default categories with explicit UUID (using ON CONFLICT to avoid duplicates)
    INSERT INTO public.categories (id, user_id, name, type, color, icon)
    VALUES 
        -- Despesas
        (gen_random_uuid(), NEW.id::UUID, 'Alimentação', 'expense', '#FF6B6B', 'food'),
        (gen_random_uuid(), NEW.id::UUID, 'Transporte', 'expense', '#4ECDC4', 'transport'),
        (gen_random_uuid(), NEW.id::UUID, 'Lazer', 'expense', '#96CEB4', 'entertainment'),
        (gen_random_uuid(), NEW.id::UUID, 'Saúde', 'expense', '#FFEAA7', 'health'),
        (gen_random_uuid(), NEW.id::UUID, 'Educação', 'expense', '#DDA0DD', 'education'),
        (gen_random_uuid(), NEW.id::UUID, 'Moradia', 'expense', '#98D8C8', 'home'),
        (gen_random_uuid(), NEW.id::UUID, 'Outros', 'expense', '#B0BEC5', 'other'),
        (gen_random_uuid(), NEW.id::UUID, 'Vestuário', 'expense', '#F39C12', 'shopping'),
        (gen_random_uuid(), NEW.id::UUID, 'Serviços', 'expense', '#3498DB', 'services'),
        (gen_random_uuid(), NEW.id::UUID, 'Pets', 'expense', '#E67E22', 'pets'),
        (gen_random_uuid(), NEW.id::UUID, 'Assinaturas', 'expense', '#9B59B6', 'subscription'),
        (gen_random_uuid(), NEW.id::UUID, 'Beleza', 'expense', '#E91E63', 'beauty'),
        -- Receitas
        (gen_random_uuid(), NEW.id::UUID, 'Salário', 'income', '#45B7D1', 'salary'),
        -- Transferência
        (gen_random_uuid(), NEW.id::UUID, 'Transferência', 'transfer', '#7B68EE', 'transfer')
    ON CONFLICT (user_id, name, type) DO NOTHING;
    
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
-- Add new categories to existing users
-- ============================================================================
INSERT INTO public.categories (id, user_id, name, type, color, icon)
SELECT 
    gen_random_uuid(),
    u.id,
    category_name,
    category_type,
    category_color,
    category_icon
FROM auth.users u
CROSS JOIN (
    VALUES 
        ('Alimentação', 'expense', '#FF6B6B', 'food'),
        ('Transporte', 'expense', '#4ECDC4', 'transport'),
        ('Lazer', 'expense', '#96CEB4', 'entertainment'),
        ('Saúde', 'expense', '#FFEAA7', 'health'),
        ('Educação', 'expense', '#DDA0DD', 'education'),
        ('Moradia', 'expense', '#98D8C8', 'home'),
        ('Outros', 'expense', '#B0BEC5', 'other'),
        ('Vestuário', 'expense', '#F39C12', 'shopping'),
        ('Serviços', 'expense', '#3498DB', 'services'),
        ('Pets', 'expense', '#E67E22', 'pets'),
        ('Assinaturas', 'expense', '#9B59B6', 'subscription'),
        ('Beleza', 'expense', '#E91E63', 'beauty'),
        ('Salário', 'income', '#45B7D1', 'salary'),
        ('Transferência', 'transfer', '#7B68EE', 'transfer')
) AS categories(category_name, category_type, category_color, category_icon)
WHERE NOT EXISTS (
    SELECT 1 FROM public.categories c 
    WHERE c.user_id = u.id AND c.name = categories.category_name
);

-- ============================================================================
-- Comments
-- ============================================================================
COMMENT ON FUNCTION public.create_default_categories() IS 'Cria categorias padrão para novos usuários incluindo categoria de transferência';

