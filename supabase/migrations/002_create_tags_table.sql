-- =====================================================
-- Migration 002: Criar tabela de Tags
-- =====================================================
-- As tags permitem organizar e filtrar transações com
-- mais flexibilidade que categorias.
-- Uma transação pode ter múltiplas tags.
-- =====================================================

-- Criar tabela tags
CREATE TABLE IF NOT EXISTS public.tags (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    color TEXT DEFAULT '#357266',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índice para busca por usuário
CREATE INDEX IF NOT EXISTS idx_tags_user_id ON public.tags(user_id);

-- Índice para busca por nome (verificar duplicatas)
CREATE INDEX IF NOT EXISTS idx_tags_name ON public.tags(name);

-- Índice composto para verificar duplicatas por usuário
CREATE INDEX IF NOT EXISTS idx_tags_user_name ON public.tags(user_id, name);

-- Habilitar RLS (Row Level Security)
ALTER TABLE public.tags ENABLE ROW LEVEL SECURITY;

-- Política: Usuários só podem ver suas próprias tags
CREATE POLICY "Users can view own tags" ON public.tags
    FOR SELECT USING (auth.uid() = user_id);

-- Política: Usuários só podem inserir suas próprias tags
CREATE POLICY "Users can insert own tags" ON public.tags
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Política: Usuários só podem atualizar suas próprias tags
CREATE POLICY "Users can update own tags" ON public.tags
    FOR UPDATE USING (auth.uid() = user_id);

-- Política: Usuários só podem excluir suas próprias tags
CREATE POLICY "Users can delete own tags" ON public.tags
    FOR DELETE USING (auth.uid() = user_id);

-- Comentários
COMMENT ON TABLE public.tags IS 'Tags para organizar transações';
COMMENT ON COLUMN public.tags.id IS 'ID único da tag (UUID)';
COMMENT ON COLUMN public.tags.user_id IS 'ID do usuário proprietário';
COMMENT ON COLUMN public.tags.name IS 'Nome da tag';
COMMENT ON COLUMN public.tags.color IS 'Cor da tag em formato hex (ex: #357266)';
COMMENT ON COLUMN public.tags.created_at IS 'Data de criação da tag';
