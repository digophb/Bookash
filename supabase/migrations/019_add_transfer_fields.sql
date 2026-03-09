-- Migration: 019_add_transfer_fields
-- Date: 2026-03-09
-- Description: Adiciona campos para transferencias entre contas
-- Author: Luffy

-- ============================================================================
-- Adicionar campos de transferencia na tabela transactions
-- ============================================================================

-- Conta de origem (para transferencias)
ALTER TABLE public.transactions 
ADD COLUMN IF NOT EXISTS from_account_id UUID REFERENCES public.accounts(id) ON DELETE SET NULL;

-- Conta de destino (para transferencias)
ALTER TABLE public.transactions 
ADD COLUMN IF NOT EXISTS to_account_id UUID REFERENCES public.accounts(id) ON DELETE SET NULL;

-- Indices para performance
CREATE INDEX IF NOT EXISTS idx_transactions_from_account ON public.transactions(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_to_account ON public.transactions(to_account_id);

-- Comentarios
COMMENT ON COLUMN public.transactions.from_account_id IS 'Conta de origem em transferencias';
COMMENT ON COLUMN public.transactions.to_account_id IS 'Conta de destino em transferencias';

-- ============================================================================
-- Atualizar constraint de tipo para incluir 'transfer'
-- ============================================================================

-- Remover constraint antigo
ALTER TABLE public.transactions DROP CONSTRAINT IF EXISTS transactions_type_check;

-- Adicionar novo constraint com 'transfer'
ALTER TABLE public.transactions 
ADD CONSTRAINT transactions_type_check 
CHECK (type IN ('income', 'expense', 'transfer'));

-- ============================================================================
-- Desabilitar RLS em accounts para testes (producao: usar politicas corretas)
-- ============================================================================
ALTER TABLE public.accounts DISABLE ROW LEVEL SECURITY;
