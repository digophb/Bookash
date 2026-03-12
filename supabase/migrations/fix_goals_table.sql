-- EXECUTAR NO SUPABASE SQL EDITOR PARA CORRIGIR A TABELA GOALS
-- https://supabase.com/dashboard/project/gqbxasjoxxslpaxjqfeg/sql

-- Adicionar coluna is_enabled se não existir
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'goals' AND column_name = 'is_enabled') THEN
        ALTER TABLE public.goals ADD COLUMN is_enabled BOOLEAN DEFAULT false;
    END IF;
END $$;

-- Adicionar outras colunas que possam estar faltando
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'goals' AND column_name = 'target_amount') THEN
        ALTER TABLE public.goals ADD COLUMN target_amount DECIMAL(12,2) DEFAULT 0;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'goals' AND column_name = 'created_at') THEN
        ALTER TABLE public.goals ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'goals' AND column_name = 'updated_at') THEN
        ALTER TABLE public.goals ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
    END IF;
END $$;

-- Verificar estrutura
SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'goals';
