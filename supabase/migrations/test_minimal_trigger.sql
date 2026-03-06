-- TESTE: Trigger MÍNIMO para debugar o problema

-- 1. Remover TODOS os triggers
DROP TRIGGER IF EXISTS on_user_created ON auth.users;
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP TRIGGER IF EXISTS on_user_created_categories ON auth.users;
DROP TRIGGER IF EXISTS on_user_created_tags ON auth.users;
DROP TRIGGER IF EXISTS trg_on_new_user_created ON auth.users;

-- 2. Remover funções
DROP FUNCTION IF EXISTS public.handle_new_user() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_account() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_categories() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_tags() CASCADE;
DROP FUNCTION IF EXISTS public.on_new_user_created() CASCADE;

-- 3. Criar função MÍNIMA (só log, não insere nada)
CREATE OR REPLACE FUNCTION public.test_trigger()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = 'postgres'
LANGUAGE plpgsql
AS $$
BEGIN
    -- Não faz nada, só retorna
    RAISE NOTICE 'Trigger executado para usuario: %', NEW.email;
    RETURN NEW;
END;
$$;

-- 4. Criar trigger mínimo
CREATE TRIGGER trg_test
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.test_trigger();

-- 5. Verificar
SELECT 'Trigger de teste criado!' as status;
