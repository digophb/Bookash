-- Migration: Tags Padrao para Novos Usuarios
-- Description: Cria automaticamente tags padrao para cada novo usuario
-- Author: Luffy (TED + ACE + NICK Agent)
-- Date: 2026-03-06
-- Card: FEATURE - Tags Padrao para Novos Usuarios

-- ============================================================================
-- STEP 1: Criar funcao para criar tags padrao
-- ============================================================================

CREATE OR REPLACE FUNCTION public.create_default_tags()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = 'postgres'
LANGUAGE plpgsql
AS $$
BEGIN
    -- Inserir tags padrao para o novo usuario
    INSERT INTO public.tags (user_id, name, color)
    VALUES 
        (NEW.id, 'Uber', '#000000'),
        (NEW.id, 'Onibus', '#4ECDC4'),
        (NEW.id, 'FastFood', '#FF9800'),
        (NEW.id, 'Restaurante', '#E53935'),
        (NEW.id, 'Cursinho', '#9C27B0'),
        (NEW.id, 'Farmacia', '#4CAF50'),
        (NEW.id, 'Day Trade', '#FFD700'),
        (NEW.id, 'Passeio', '#03A9F4'),
        (NEW.id, 'Presente', '#E91E63'),
        (NEW.id, 'Roupas', '#D81B60');
    
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Erro no create_default_tags: %', SQLERRM;
    RETURN NEW;
END;
$$;

-- ============================================================================
-- STEP 2: Criar trigger para disparar apos insercao de usuario
-- ============================================================================

-- Remover trigger existente se houver
DROP TRIGGER IF EXISTS on_user_created_tags ON auth.users;

-- Criar trigger que dispara apos INSERT na tabela auth.users
CREATE TRIGGER on_user_created_tags
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.create_default_tags();

-- ============================================================================
-- STEP 3: Comments para documentacao
-- ============================================================================

COMMENT ON FUNCTION public.create_default_tags() IS 'Cria tags padrao para novos usuarios';

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. A funcao usa SECURITY DEFINER para ter permissoes elevadas
-- 2. O trigger dispara APOS o INSERT em auth.users (apos usuario ser criado)
-- 3. Cada novo usuario recebe automaticamente 10 tags padrao
-- 4. O usuario pode editar ou excluir suas tags depois
-- 5. Tags sao criadas com user_id do novo usuario (isolamento total)
-- 6. Tags padrao: Uber, Onibus, FastFood, Restaurante, Cursinho, 
--    Farmacia, Day Trade, Passeio, Presente, Roupas
