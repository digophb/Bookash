-- Migration: Create public.users table
-- Description: Cria tabela de usuários no schema public para dados adicionais
-- Author: TED (Backend Agent)
-- Date: 2026-03-03
-- Card: #12 - FEATURE - Conta Padrão 'Carteira' para Novos Usuários

-- ============================================================================
-- STEP 1: Criar tabela users no schema public
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    name TEXT,
    avatar_url TEXT,
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

-- Remover políticas existentes antes de criar
DROP POLICY IF EXISTS "Users can view own data" ON public.users;
DROP POLICY IF EXISTS "Users can update own data" ON public.users;

-- Usuários podem ver apenas seus próprios dados
CREATE POLICY "Users can view own data" ON public.users
    FOR SELECT USING (auth.uid() = id);

-- Usuários podem atualizar apenas seus próprios dados
CREATE POLICY "Users can update own data" ON public.users
    FOR UPDATE USING (auth.uid() = id);

-- INSERT é feito via trigger ou função com SECURITY DEFINER
-- Não permitimos INSERT direto via API

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
-- STEP 6: Criar função para inserir usuário automaticamente
-- ============================================================================

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.users (id, email, name)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'name', 'Usuário')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- STEP 7: Criar trigger para inserir usuário após signup
-- ============================================================================

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- ============================================================================
-- STEP 8: Comments
-- ============================================================================

COMMENT ON TABLE public.users IS 'Dados adicionais dos usuários (nome, avatar, etc.)';
COMMENT ON COLUMN public.users.id IS 'Referência ao usuário no auth.users';
COMMENT ON FUNCTION public.handle_new_user() IS 'Insere automaticamente na tabela public.users após signup';

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. A tabela armazena dados adicionais do usuário (nome, avatar)
-- 2. O id é referência à tabela auth.users do Supabase
-- 3. RLS garante que cada usuário só acessa seus próprios dados
-- 4. INSERT é feito via trigger ou função com permissões elevadas
