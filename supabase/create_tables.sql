-- Tabela de Contas
CREATE TABLE IF NOT EXISTS public.accounts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    balance DOUBLE PRECISION DEFAULT 0,
    type TEXT DEFAULT 'corrente',
    icon TEXT DEFAULT 'wallet',
    color TEXT DEFAULT '#357266',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Tabela de Transações
CREATE TABLE IF NOT EXISTS public.transactions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL, -- 'income', 'expense', 'transfer'
    amount DOUBLE PRECISION NOT NULL,
    description TEXT,
    category TEXT,
    category_id UUID REFERENCES public.categories(id),
    account_id UUID REFERENCES public.accounts(id),
    date DATE NOT NULL,
    status TEXT DEFAULT 'paid', -- 'paid', 'pending'
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_period TEXT, -- 'daily', 'weekly', 'monthly', 'yearly'
    recurrence_count INTEGER DEFAULT 1,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON public.accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON public.transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON public.transactions(date);
CREATE INDEX IF NOT EXISTS idx_transactions_category ON public.transactions(category);

-- Desabilitar RLS (Row Level Security) para desenvolvimento
ALTER TABLE public.accounts DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions DISABLE ROW LEVEL SECURITY;

-- Políticas de acesso (opcional, se quiser ativar RLS depois)
-- ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;
-- CREATE POLICY "Users can view their own accounts" ON public.accounts FOR SELECT USING (auth.uid() = user_id);
-- CREATE POLICY "Users can insert their own accounts" ON public.accounts FOR INSERT WITH CHECK (auth.uid() = user_id);
-- CREATE POLICY "Users can update their own accounts" ON public.accounts FOR UPDATE USING (auth.uid() = user_id);
-- CREATE POLICY "Users can delete their own accounts" ON public.accounts FOR DELETE USING (auth.uid() = user_id);
