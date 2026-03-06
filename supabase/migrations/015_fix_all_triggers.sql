-- Migration: FIX - Triggers consolidados para criação de usuário
-- Description: Remove triggers conflitantes e cria um único trigger
-- Date: 2026-03-06

-- =====================================================
-- PASSO 1: REMOVER TODOS OS TRIGGERS EXISTENTES
-- =====================================================
DROP TRIGGER IF EXISTS on_user_created ON auth.users;
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP TRIGGER IF EXISTS on_user_created_categories ON auth.users;
DROP TRIGGER IF EXISTS on_user_created_tags ON auth.users;

-- =====================================================
-- PASSO 2: REMOVER FUNÇÕES EXISTENTES
-- =====================================================
DROP FUNCTION IF EXISTS public.handle_new_user() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_account() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_categories() CASCADE;
DROP FUNCTION IF EXISTS public.create_default_tags() CASCADE;

-- =====================================================
-- PASSO 3: CRIAR FUNÇÃO ÚNICA CONSOLIDADA
-- =====================================================
CREATE OR REPLACE FUNCTION public.on_new_user_created()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = 'postgres'
LANGUAGE plpgsql
AS $$
BEGIN
    -- 1. Inserir na tabela public.users
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
    
    -- 2. Inserir contas padrao (Carteira + Banco)
    INSERT INTO public.accounts (user_id, name, balance, type, icon)
    VALUES 
        (NEW.id, 'Carteira', 0, 'dinheiro', 'wallet'),
        (NEW.id, 'Banco', 0, 'corrente', 'bank');
    
    -- 3. Inserir categorias padrao
    INSERT INTO public.categories (user_id, name, type, color, icon)
    VALUES 
        (NEW.id, 'Alimentacao', 'expense', '#FF6B6B', 'restaurant'),
        (NEW.id, 'Transporte', 'expense', '#4ECDC4', 'directions_car'),
        (NEW.id, 'Salario', 'income', '#45B7D1', 'attach_money'),
        (NEW.id, 'Lazer', 'expense', '#96CEB4', 'sports_esports'),
        (NEW.id, 'Saude', 'expense', '#FFEAA7', 'local_hospital'),
        (NEW.id, 'Educacao', 'expense', '#DDA0DD', 'school'),
        (NEW.id, 'Moradia', 'expense', '#98D8C8', 'home'),
        (NEW.id, 'Outros', 'expense', '#B0BEC5', 'more_horiz');
    
    -- 4. Inserir tags padrao
    INSERT INTO public.tags (user_id, name, color)
    VALUES 
        (NEW.id, 'Uber', '#000000'),
        (NEW.id, 'Onibus', '#4ECDC4'),
        (NEW.id, 'FastFood', '#FF9800'),
        (NEW.id, 'Restaurante', '#E53935'),
        (NEW.id, 'Cursinho', '#9C27B0'),
        (NEW.id, 'Farmacia', '#4CAF50'),
        (NEW.id, 'Day Trade', '#FFD700'),
        (NEW.id, 'Passeio', '#03A9F4'),
        (NEW.id, 'Presente', '#E91E63'),
        (NEW.id, 'Roupas', '#D81B60');
    
    RETURN NEW;
END;
$$;

-- =====================================================
-- PASSO 4: CRIAR TRIGGER ÚNICO
-- =====================================================
CREATE TRIGGER trg_on_new_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.on_new_user_created();

-- =====================================================
-- PASSO 5: VERIFICAR
-- =====================================================
SELECT 'Trigger criado com sucesso!' as status;
