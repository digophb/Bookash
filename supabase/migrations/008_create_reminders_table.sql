-- Migration 008: Criar tabela de lembretes de transações
--
-- Tabela para armazenar lembretes de transações recorrentes ou futuras

CREATE TABLE IF NOT EXISTS public.reminders (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    transaction_id UUID REFERENCES public.transactions(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    amount DOUBLE PRECISION,
    reminder_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_type TEXT, -- 'daily', 'weekly', 'monthly', 'yearly'
    recurrence_interval INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    last_triggered_at TIMESTAMP WITH TIME ZONE,
    next_trigger_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_reminders_user_id ON public.reminders(user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_date ON public.reminders(reminder_date);
CREATE INDEX IF NOT EXISTS idx_reminders_next_trigger ON public.reminders(next_trigger_at);
CREATE INDEX IF NOT EXISTS idx_reminders_active ON public.reminders(is_active);

-- RLS desabilitado por padrão (ajuste conforme necessário)
ALTER TABLE public.reminders DISABLE ROW LEVEL SECURITY;

-- Trigger para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION public.update_reminders_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_reminders_updated_at
    BEFORE UPDATE ON public.reminders
    FOR EACH ROW
    EXECUTE FUNCTION public.update_reminders_updated_at();

-- Comentários para documentação
COMMENT ON TABLE public.reminders IS 'Lembretes de transações - permite agendar notificações para transações futuras ou recorrentes';
COMMENT ON COLUMN public.reminders.transaction_id IS 'Referência opcional para uma transação existente';
COMMENT ON COLUMN public.reminders.recurrence_type IS 'Tipo de recorrência: daily, weekly, monthly, yearly';
COMMENT ON COLUMN public.reminders.recurrence_interval IS 'Intervalo da recorrência (ex: a cada 2 meses)';
