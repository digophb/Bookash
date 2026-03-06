-- Migration: Categorias Padrao para Novos Usuarios
-- Execute este SQL no Supabase SQL Editor

CREATE OR REPLACE FUNCTION public.create_default_categories()
RETURNS TRIGGER 
SECURITY DEFINER
SET role = postgres
LANGUAGE plpgsql
AS $$
BEGIN
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
    
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Erro: %', SQLERRM;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_user_created_categories ON auth.users;

CREATE TRIGGER on_user_created_categories
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.create_default_categories();

COMMENT ON FUNCTION public.create_default_categories() IS 'Cria categorias padrao para novos usuarios';
