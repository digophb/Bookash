-- Migration: Contas Padrao Carteira e Banco para Novos Usuarios
-- Description: Cria automaticamente contas Carteira e Banco para cada novo usuario
-- Author: Luffy (TED + ACE + NICK Agent)
-- Date: 2026-03-05
-- Card: FEATURE - Conta Padrao Banco para Novos Usuarios

-- ============================================================================
-- STEP 1: Atualizar funcao para criar contas padrao (Carteira + Banco)
-- ============================================================================

CREATE OR REPLACE FUNCTION public.create_default_account()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = 'postgres'
LANGUAGE plpgsql
AS $$
BEGIN
    -- Inserir conta Carteira para o novo usuario
    INSERT INTO public.accounts (user_id, name, balance, type, icon)
    VALUES (
        NEW.id,
        'Carteira',
        0,
        'dinheiro',
        'wallet'
    );
    
    -- Inserir conta Banco para o novo usuario
    INSERT INTO public.accounts (user_id, name, balance, type, icon)
    VALUES (
        NEW.id,
        'Banco',
        0,
        'corrente',
        'bank'
    );
    
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Erro no create_default_account: %', SQLERRM;
    RETURN NEW;
END;
$$;

-- ============================================================================
-- STEP 2: Comments para documentacao
-- ============================================================================

COMMENT ON FUNCTION public.create_default_account() IS 'Funcao trigger que cria automaticamente contas Carteira e Banco para novos usuarios';

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. A funcao usa SECURITY DEFINER para ter permissoes elevadas
-- 2. O trigger dispara APOS o INSERT em auth.users (apos usuario ser criado)
-- 3. Cada novo usuario recebe automaticamente duas contas: Carteira e Banco
-- 4. O usuario pode editar ou excluir suas contas depois
-- 5. Esta migration atualiza a funcao existente para incluir a conta Banco
