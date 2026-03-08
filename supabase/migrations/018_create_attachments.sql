-- Migration: 018_create_attachments
-- Date: 2026-03-08
-- Description: Tabela de anexos (fotos, documentos) para transacoes
-- Author: Luffy

-- ============================================================================
-- Criar tabela attachments
-- ============================================================================
-- Permite anexar fotos, recibos, documentos a transacoes

CREATE TABLE IF NOT EXISTS public.attachments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES public.transactions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Dados do arquivo
    file_name TEXT,
    file_url TEXT NOT NULL,          -- URL do arquivo no Supabase Storage
    file_type TEXT DEFAULT 'image',  -- image, document, pdf, etc
    file_size INTEGER,               -- tamanho em bytes
    mime_type TEXT,                  -- image/jpeg, application/pdf, etc
    
    -- Metadados
    description TEXT,                -- descricao opcional do anexo
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indices para performance
CREATE INDEX IF NOT EXISTS idx_attachments_transaction_id ON public.attachments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_attachments_user_id ON public.attachments(user_id);
CREATE INDEX IF NOT EXISTS idx_attachments_file_type ON public.attachments(file_type);

-- RLS (Row Level Security)
ALTER TABLE public.attachments ENABLE ROW LEVEL SECURITY;

-- Politica: Usuarios podem ver anexos de suas proprias transacoes
CREATE POLICY "Users can view own attachments" ON public.attachments
    FOR SELECT USING (auth.uid() = user_id);

-- Politica: Usuarios podem inserir anexos em suas proprias transacoes
CREATE POLICY "Users can insert own attachments" ON public.attachments
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Politica: Usuarios podem atualizar seus proprios anexos
CREATE POLICY "Users can update own attachments" ON public.attachments
    FOR UPDATE USING (auth.uid() = user_id);

-- Politica: Usuarios podem excluir seus proprios anexos
CREATE POLICY "Users can delete own attachments" ON public.attachments
    FOR DELETE USING (auth.uid() = user_id);

-- Comentarios
COMMENT ON TABLE public.attachments IS 'Anexos (fotos, documentos) de transacoes';
COMMENT ON COLUMN public.attachments.transaction_id IS 'ID da transacao associada';
COMMENT ON COLUMN public.attachments.user_id IS 'ID do usuario proprietario';
COMMENT ON COLUMN public.attachments.file_name IS 'Nome original do arquivo';
COMMENT ON COLUMN public.attachments.file_url IS 'URL do arquivo no Supabase Storage';
COMMENT ON COLUMN public.attachments.file_type IS 'Tipo: image, document, pdf, etc';
COMMENT ON COLUMN public.attachments.file_size IS 'Tamanho do arquivo em bytes';
COMMENT ON COLUMN public.attachments.mime_type IS 'MIME type do arquivo';
COMMENT ON COLUMN public.attachments.description IS 'Descricao opcional do anexo';

-- ============================================================================
-- Criar bucket no Supabase Storage (se nao existir)
-- ============================================================================
-- Nota: Execute manualmente no Supabase Dashboard > Storage > New bucket
-- Nome: attachments
-- Publico: NAO (usar signed URLs)

-- Trigger para atualizar updated_at
CREATE OR REPLACE FUNCTION update_attachments_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_attachments_updated_at
    BEFORE UPDATE ON public.attachments
    FOR EACH ROW
    EXECUTE FUNCTION update_attachments_updated_at();
