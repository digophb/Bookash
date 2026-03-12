-- ============================================================================
-- CORREÇÃO COMPLETA: Triggers de criação de usuário
-- Execute no Supabase SQL Editor
-- ============================================================================

-- PASSO 1: Remover TODOS os triggers existentes
DROP TRIGGER IF EXISTS on_user_created ON auth.users;
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP TRIGGER IF EXISTS on_user_created_categories ON auth.users;
DROP TRIGGER IF EXISTS on_user_created_tags ON auth.users;
DROP TRIGGER IF EXISTS trg_on_new_user_created ON auth.users;

-- PASSO 2: Remover TODAS as funções existentes
DROP FUNCTION IF EXISTS public.handle_new_user() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_account() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_categories() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_tags() CASCADE;
DROP FUNCTION IF EXISTS public.on_new_user_created() CASCADE;

-- PASSO 3: Adicionar tipo 'transfer' à constraint
ALTER TABLE public.categories DROP CONSTRAINT IF EXISTS categories_type_check;
ALTER TABLE public.categories ADD CONSTRAINT categories_type_check 
CHECK (((type)::text = ANY ((ARRAY['income'::character varying, 'expense'::character varying, 'transfer'::character varying])::text[])));

-- PASSO 4: Criar função consolidada que cria APENAS o perfil do usuário
-- O app cria categorias, contas e tags manualmente
CREATE OR REPLACE FUNCTION public.on_new_user_created()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = postgres
LANGUAGE plpgsql
AS $$
BEGIN
    -- Inserir APENAS na tabela public.users
    -- O app cria categorias, contas e tags manualmente
    INSERT INTO public.users (id, email, name)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(
            NEW.raw_user_meta_data->>'name',
            split_part(NEW.email, '@', 1),
            'Usuario'
        )
    );
    
    RETURN NEW;
END;
$$;

-- PASSO 5: Criar trigger único
CREATE TRIGGER trg_on_new_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.on_new_user_created();

-- PASSO 6: Verificar
SELECT 'Trigger criado com sucesso!' as status;

-- ============================================================================
-- Verificar triggers ativos
-- ============================================================================
SELECT tgname, proname 
FROM pg_trigger t 
JOIN pg_proc p ON t.tgfoid = p.oid 
WHERE tgrelid = 'auth.users'::regclass;
