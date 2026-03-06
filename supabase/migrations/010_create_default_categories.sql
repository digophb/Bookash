-- Migration: Categorias Padrao Isoladas por Usuario
-- Description: Cria automaticamente categorias padrao para cada novo usuario
-- Author: Luffy (TED + ACE + NICK Agent)
-- Date: 2026-03-05
-- Card: FEATURE - Categorias Padrao Isoladas por Usuario

-- ============================================================================
-- STEP 1: Criar funcao para criar categorias padrao
-- ============================================================================

CREATE OR REPLACE FUNCTION public.create_default_categories()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = 'postgres'
LANGUAGE plpgsql
AS $$
BEGIN
    -- Inserir categorias padrao para o novo usuario
    INSERT INTO public.categories (user_id, name, type, color, icon)
    VALUES 
        (NEW.id, 'Alimentacao', 'expense', '#FF6B6B', 'restaurant'),
        (NEW.id, 'Transporte', 'expense', '#4ECDC4', 'directions_car'),
        (NEW.id, 'Salario', 'income', '#45B7D1', 'attach_money'),
        (NEW.id, 'Lazer', 'expense', '#96CEB4', 'sports_esports'),
        (NEW.id, 'Saude', 'expense', '#FFEAA7', 'local_hospital'),
        (NEW.id, 'Educacao', 'expense', '#DDA0DD', 'school'),
        (NEW.id, 'Moradia', 'expense', '#98D8C8', 'home'),
        (NEW.id, 'Outros', 'expense', '#B0BEC5', 'more_horiz');
    
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Erro no create_default_categories: %', SQLERRM;
    RETURN NEW;
END;
$$;

-- ============================================================================
-- STEP 2: Criar trigger para disparar apos insercao de usuario
-- ============================================================================

-- Remover trigger existente se houver
DROP TRIGGER IF EXISTS on_user_created_categories ON auth.users;

-- Criar trigger que dispara apos INSERT na tabela auth.users
CREATE TRIGGER on_user_created_categories
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.create_default_categories();

-- ============================================================================
-- STEP 3: Comments para documentacao
-- ============================================================================

COMMENT ON FUNCTION public.create_default_categories() IS 'Cria categorias padrao para novos usuarios';

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. A funcao usa SECURITY DEFINER para ter permissoes elevadas
-- 2. O trigger dispara APOS o INSERT em auth.users (apos usuario ser criado)
-- 3. Cada novo usuario recebe automaticamente 8 categorias padrao
-- 4. O usuario pode editar ou excluir suas categorias depois
-- 5. Categorias sao criadas com user_id do novo usuario (isolamento total)
