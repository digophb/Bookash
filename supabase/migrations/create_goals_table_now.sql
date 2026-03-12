-- EXECUTAR ESTE SQL NO SUPABASE SQL EDITOR
-- https://supabase.com/dashboard/project/gqbxasjoxxslpaxjqfeg/sql

-- Criar tabela de metas de ganhos
CREATE TABLE IF NOT EXISTS public.goals (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('daily', 'weekly', 'monthly', 'yearly')),
    target_amount DECIMAL(12,2) DEFAULT 0,
    is_enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT unique_user_goal_type UNIQUE (user_id, type)
);

-- Criar índice
CREATE INDEX IF NOT EXISTS idx_goals_user_id ON public.goals(user_id);

-- Habilitar RLS
ALTER TABLE public.goals ENABLE ROW LEVEL SECURITY;

-- Criar políticas
CREATE POLICY goals_select ON public.goals FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY goals_insert ON public.goals FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY goals_update ON public.goals FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY goals_delete ON public.goals FOR DELETE USING (auth.uid() = user_id);

-- Comentário
COMMENT ON TABLE public.goals IS 'Metas de ganhos dos usuarios';
