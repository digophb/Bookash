-- Migration: Adicionar campo include_in_balance na tabela accounts
-- Description: Permite controlar se o saldo da conta deve ser incluido no total do dashboard
-- Author: Luffy (TED + ACE + NICK Agent)
-- Date: 2026-03-05
-- Card: FEATURE - Padronizacao UI e Melhorias Gerais

-- ============================================================================
-- STEP 1: Adicionar coluna include_in_balance
-- ============================================================================

ALTER TABLE public.accounts
ADD COLUMN IF NOT EXISTS include_in_balance BOOLEAN DEFAULT true;

-- ============================================================================
-- STEP 2: Criar indice para consultas
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_accounts_include_in_balance
ON public.accounts(user_id, include_in_balance)
WHERE include_in_balance = true;

-- ============================================================================
-- STEP 3: Comment para documentacao
-- ============================================================================

COMMENT ON COLUMN public.accounts.include_in_balance IS
'Indica se o saldo desta conta deve ser incluido no total do dashboard';

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. Por padrao, todas as contas existentes terao include_in_balance = true
-- 2. O usuario pode desativar para contas que nao deseja ver no saldo total
-- 3. Exemplo: conta de investimento que nao deve afetar o saldo do dia a dia
