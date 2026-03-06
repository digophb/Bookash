-- Migration: Recriar tabela public.users
-- Description: Recria a tabela users após remoção para debug
-- Date: 2026-03-06

-- ============================================================================
-- STEP 1: Criar tabela users
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    name TEXT NOT NULL DEFAULT 'Usuario',
    avatar_url TEXT,
    is_premium BOOLEAN DEFAULT FALSE,
    premium_expires_at TIMESTAMP WITH TIME ZONE,
    stripe_customer_id TEXT,
    stripe_subscription_id TEXT,
    settings JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================================================
-- STEP 2: Criar índices
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(email);

-- ============================================================================
-- STEP 3: Habilitar RLS
-- ============================================================================
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- STEP 4: Criar políticas RLS
-- ============================================================================
DROP POLICY IF EXISTS "Users can view own data" ON public.users;
DROP POLICY IF EXISTS "Users can update own data" ON public.users;
DROP POLICY IF EXISTS "Users can insert own data" ON public.users;

-- Usuários podem ver apenas seus próprios dados
CREATE POLICY "Users can view own data" ON public.users
    FOR SELECT USING (auth.uid() = id);

-- Usuários podem atualizar apenas seus próprios dados
CREATE POLICY "Users can update own data" ON public.users
    FOR UPDATE USING (auth.uid() = id);

-- Permitir INSERT via função com SECURITY DEFINER
CREATE POLICY "Users can insert own data" ON public.users
    FOR INSERT WITH CHECK (auth.uid() = id);

-- ============================================================================
-- STEP 5: Criar trigger para atualizar updated_at
-- ============================================================================
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_users_updated_at ON public.users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- ============================================================================
-- STEP 6: Comments
-- ============================================================================
COMMENT ON TABLE public.users IS 'Dados adicionais dos usuários (nome, avatar, premium, etc.)';
COMMENT ON COLUMN public.users.id IS 'Referência ao usuário no auth.users';
COMMENT ON COLUMN public.users.email IS 'Email do usuário';
COMMENT ON COLUMN public.users.name IS 'Nome do usuário';
COMMENT ON COLUMN public.users.avatar_url IS 'URL do avatar do usuário';
COMMENT ON COLUMN public.users.is_premium IS 'Se o usuário é premium';
COMMENT ON COLUMN public.users.premium_expires_at IS 'Data de expiração do premium';
COMMENT ON COLUMN public.users.stripe_customer_id IS 'ID do cliente no Stripe';
COMMENT ON COLUMN public.users.stripe_subscription_id IS 'ID da assinatura no Stripe';
COMMENT ON COLUMN public.users.settings IS 'Configurações do usuário em JSON';

-- ============================================================================
-- STEP 7: Re-habilitar RLS nas outras tabelas
-- ============================================================================
ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tags ENABLE ROW LEVEL SECURITY;

SELECT 'Tabela users recriada com sucesso!' as status;
