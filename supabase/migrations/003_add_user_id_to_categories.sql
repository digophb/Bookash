-- Migration: Add user_id to categories for default + personal categories
-- Description: Enables default categories (user_id = NULL) visible to all users
--              and personal categories (user_id = user's id) specific to each user
-- Author: ACE (Integration Agent)
-- Date: 2026-03-02

-- Step 1: Add user_id column if not exists (nullable for default categories)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'categories' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE public.categories ADD COLUMN user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Step 2: Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_categories_user_id ON public.categories(user_id);

-- Step 3: Create composite index for common query pattern
CREATE INDEX IF NOT EXISTS idx_categories_user_id_type ON public.categories(user_id, type);

-- Step 4: Update RLS policies (if RLS is enabled)
-- Allow users to see default categories (user_id IS NULL) AND their own categories
-- Note: Run this in Supabase Dashboard if RLS is enabled

/*
-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view their own categories" ON public.categories;
DROP POLICY IF EXISTS "Users can insert their own categories" ON public.categories;
DROP POLICY IF EXISTS "Users can update their own categories" ON public.categories;
DROP POLICY IF EXISTS "Users can delete their own categories" ON public.categories;

-- New policies
CREATE POLICY "Users can view default and own categories"
    ON public.categories FOR SELECT
    USING (user_id IS NULL OR user_id = auth.uid());

CREATE POLICY "Users can insert own categories"
    ON public.categories FOR INSERT
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own categories only"
    ON public.categories FOR UPDATE
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can delete own categories only"
    ON public.categories FOR DELETE
    USING (user_id = auth.uid());
*/

-- Step 5: Comments for documentation
COMMENT ON COLUMN public.categories.user_id IS 'Owner user ID. NULL = default category visible to all users. Non-NULL = personal category specific to user';

-- Step 6: Verify existing default categories
-- Run this query to see current categories that should be default
-- UPDATE public.categories SET user_id = NULL WHERE name IN ('Alimentação', 'Transporte', 'Salário', 'Saúde', 'Educação', 'Lazer');
