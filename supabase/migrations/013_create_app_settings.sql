-- Migration: Criar tabela de configuracoes do app
-- Date: 2026-03-06

-- Criar tabela app_settings
CREATE TABLE IF NOT EXISTS public.app_settings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,
    theme TEXT DEFAULT 'system',
    language TEXT DEFAULT 'pt-BR',
    notifications_enabled BOOLEAN DEFAULT true,
    currency TEXT DEFAULT 'BRL',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Criar indice
CREATE INDEX IF NOT EXISTS idx_app_settings_user_id ON public.app_settings(user_id);

-- Habilitar RLS
ALTER TABLE public.app_settings ENABLE ROW LEVEL SECURITY;

-- Criar politicas RLS
CREATE POLICY app_settings_select_policy ON public.app_settings FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY app_settings_insert_policy ON public.app_settings FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY app_settings_update_policy ON public.app_settings FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY app_settings_delete_policy ON public.app_settings FOR DELETE USING (auth.uid() = user_id);

-- Trigger para updated_at (reutiliza funcao existente)
DROP TRIGGER IF EXISTS update_app_settings_updated_at ON public.app_settings;
CREATE TRIGGER update_app_settings_updated_at
BEFORE UPDATE ON public.app_settings
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
