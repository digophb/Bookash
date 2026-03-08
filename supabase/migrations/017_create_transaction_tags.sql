-- Migration: 017_create_transaction_tags
-- Date: 2026-03-08
-- Description: Tabela de relacionamento N:N entre transacoes e tags
-- Author: Luffy

-- ============================================================================
-- Criar tabela transaction_tags
-- ============================================================================
-- Permite que uma transacao tenha multiplas tags e uma tag seja usada em
-- multiplas transacoes (relacionamento N:N)

CREATE TABLE IF NOT EXISTS public.transaction_tags (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES public.transactions(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES public.tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Constraint unica: nao permitir duplicar relacionamento
    CONSTRAINT unique_transaction_tag UNIQUE (transaction_id, tag_id)
);

-- Indices para performance
CREATE INDEX IF NOT EXISTS idx_transaction_tags_transaction_id ON public.transaction_tags(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_tags_tag_id ON public.transaction_tags(tag_id);

-- RLS (Row Level Security)
ALTER TABLE public.transaction_tags ENABLE ROW LEVEL SECURITY;

-- Politica: Usuarios podem ver tags de suas proprias transacoes
CREATE POLICY "Users can view own transaction tags" ON public.transaction_tags
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.transactions 
            WHERE transactions.id = transaction_tags.transaction_id 
            AND transactions.user_id = auth.uid()
        )
    );

-- Politica: Usuarios podem inserir tags em suas proprias transacoes
CREATE POLICY "Users can insert own transaction tags" ON public.transaction_tags
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.transactions 
            WHERE transactions.id = transaction_tags.transaction_id 
            AND transactions.user_id = auth.uid()
        )
        AND EXISTS (
            SELECT 1 FROM public.tags 
            WHERE tags.id = transaction_tags.tag_id 
            AND tags.user_id = auth.uid()
        )
    );

-- Politica: Usuarios podem excluir tags de suas proprias transacoes
CREATE POLICY "Users can delete own transaction tags" ON public.transaction_tags
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM public.transactions 
            WHERE transactions.id = transaction_tags.transaction_id 
            AND transactions.user_id = auth.uid()
        )
    );

-- Comentarios
COMMENT ON TABLE public.transaction_tags IS 'Relacionamento N:N entre transacoes e tags';
COMMENT ON COLUMN public.transaction_tags.transaction_id IS 'ID da transacao';
COMMENT ON COLUMN public.transaction_tags.tag_id IS 'ID da tag';
COMMENT ON COLUMN public.transaction_tags.created_at IS 'Data de criacao do relacionamento';
