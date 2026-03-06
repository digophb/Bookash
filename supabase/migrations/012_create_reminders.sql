-- Migration: Criar tabela de lembretes
-- Date: 2026-03-05

-- Criar tabela reminders
CREATE TABLE IF NOT EXISTS public.reminders (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    transaction_id UUID REFERENCES public.transactions(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    amount DECIMAL(12,2),
    reminder_date DATE NOT NULL,
    reminder_time TIME,
    is_recurring BOOLEAN DEFAULT false,
    recurrence_type TEXT,
    category_id UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    account_id UUID REFERENCES public.accounts(id) ON DELETE SET NULL,
    is_completed BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Criar indices
CREATE INDEX IF NOT EXISTS idx_reminders_user_id ON public.reminders(user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_date ON public.reminders(reminder_date);

-- Habilitar RLS
ALTER TABLE public.reminders ENABLE ROW LEVEL SECURITY;

-- Criar politicas RLS
CREATE POLICY reminders_select_policy ON public.reminders FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY reminders_insert_policy ON public.reminders FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY reminders_update_policy ON public.reminders FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY reminders_delete_policy ON public.reminders FOR DELETE USING (auth.uid() = user_id);

-- Trigger para updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_reminders_updated_at
BEFORE UPDATE ON public.reminders
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
