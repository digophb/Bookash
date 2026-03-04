-- Migration 007: Adicionar campo include_in_balance na tabela accounts
-- 
-- Este campo controla se o saldo da conta deve ser incluído no total do dashboard

ALTER TABLE public.accounts 
ADD COLUMN IF NOT EXISTS include_in_balance BOOLEAN DEFAULT TRUE;

-- Adicionar comentário documentando o campo
COMMENT ON COLUMN public.accounts.include_in_balance IS 'Se TRUE, o saldo desta conta é incluído no total do dashboard';

-- Atualizar contas existentes para TRUE (padrão)
UPDATE public.accounts 
SET include_in_balance = TRUE 
WHERE include_in_balance IS NULL;
