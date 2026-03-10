-- Migration: 020_add_account_id_to_transactions
-- Date: 2026-03-10
-- Description: Adiciona campo account_id para vincular transacoes a contas
-- Author: Luffy

-- ============================================================================
-- Adicionar coluna account_id na tabela transactions
-- ============================================================================

ALTER TABLE public.transactions 
ADD COLUMN IF NOT EXISTS account_id UUID REFERENCES public.accounts(id) ON DELETE SET NULL;

-- Indice para performance
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON public.transactions(account_id);

-- Comentario
COMMENT ON COLUMN public.transactions.account_id IS 'Conta associada a transacao de receita ou despesa';
