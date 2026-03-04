-- Migration: Conta Padrão 'Carteira' para Novos Usuários
-- Description: Cria automaticamente uma conta 'Carteira' para cada novo usuário
-- Author: TED (Backend Agent)
-- Date: 2026-03-02
-- Card: #12 - FEATURE - Conta Padrão 'Carteira' para Novos Usuários

-- ============================================================================
-- STEP 1: Criar função para criar conta padrão
-- ============================================================================

CREATE OR REPLACE FUNCTION public.create_default_account()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = 'postgres'
LANGUAGE plpgsql
AS $$
BEGIN
    -- Inserir conta 'Carteira' para o novo usuário
    INSERT INTO public.accounts (user_id, name, balance, type, icon)
    VALUES (
        NEW.id,                    -- user_id do novo usuário
        'Carteira',                -- nome padrão
        0,                         -- saldo inicial
        'dinheiro',                -- tipo
        'wallet'                   -- ícone
    );
    
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    -- Se falhar, loga mas não impede o cadastro
    RAISE NOTICE 'Erro no create_default_account: %', SQLERRM;
    RETURN NEW;
END;
$$;

-- ============================================================================
-- STEP 2: Criar trigger para disparar após inserção de usuário
-- ============================================================================

-- Remover trigger existente se houver
DROP TRIGGER IF EXISTS on_user_created ON auth.users;

-- Criar trigger que dispara após INSERT na tabela auth.users
CREATE TRIGGER on_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.create_default_account();

-- ============================================================================
-- STEP 3: Criar conta para usuários existentes (opcional)
-- ============================================================================

-- Descomente a linha abaixo para criar conta para usuários existentes que não têm conta
-- INSERT INTO public.accounts (user_id, name, balance, type, icon)
-- SELECT id, 'Carteira', 0, 'dinheiro', 'wallet'
-- FROM auth.users u
-- WHERE NOT EXISTS (
--     SELECT 1 FROM public.accounts a WHERE a.user_id = u.id
-- );

-- ============================================================================
-- STEP 4: Comments para documentação
-- ============================================================================

COMMENT ON FUNCTION public.create_default_account() IS 
'Função trigger que cria automaticamente uma conta Carteira para novos usuários';

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. A função usa SECURITY DEFINER para ter permissões elevadas
-- 2. O trigger dispara APÓS o INSERT em auth.users (após usuário ser criado)
-- 3. Cada novo usuário recebe automaticamente uma conta 'Carteira'
-- 4. O usuário pode editar ou excluir sua conta Carteira depois
-- 5. Para criar contas para usuários existentes, descomente o INSERT em STEP 3
