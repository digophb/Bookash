-- Migration: Add user_id for multi-user isolation
-- Date: 2026-02-28
-- Priority: P0 - CRITICAL

-- ============================================================================
-- STEP 1: Add user_id column to tables that don't have it
-- ============================================================================

-- Categories: Add user_id
ALTER TABLE public.categories 
ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);

-- Accounts: Add user_id (if not exists)
ALTER TABLE public.accounts 
ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);

-- Tags: Add user_id
ALTER TABLE public.tags 
ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);

-- ============================================================================
-- STEP 2: Create indexes for user_id for better query performance
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_categories_user_id ON public.categories(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON public.accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_tags_user_id ON public.tags(user_id);

-- ============================================================================
-- STEP 3: Set default user for existing data (optional - remove after migration)
-- ============================================================================

-- WARNING: This sets a specific user_id for existing records
-- Replace 'USER_UUID_HERE' with the actual user UUID from auth.users
-- UPDATE public.categories SET user_id = 'USER_UUID_HERE' WHERE user_id IS NULL;
-- UPDATE public.accounts SET user_id = 'USER_UUID_HERE' WHERE user_id IS NULL;
-- UPDATE public.tags SET user_id = 'USER_UUID_HERE' WHERE user_id IS NULL;

-- ============================================================================
-- STEP 4: Enable RLS (Row Level Security) - RECOMMENDED
-- ============================================================================

-- Enable RLS on all tables
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for categories
CREATE POLICY "Users can view own categories" ON public.categories
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own categories" ON public.categories
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own categories" ON public.categories
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own categories" ON public.categories
    FOR DELETE USING (auth.uid() = user_id);

-- Create RLS policies for accounts
CREATE POLICY "Users can view own accounts" ON public.accounts
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own accounts" ON public.accounts
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own accounts" ON public.accounts
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own accounts" ON public.accounts
    FOR DELETE USING (auth.uid() = user_id);

-- Create RLS policies for tags
CREATE POLICY "Users can view own tags" ON public.tags
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own tags" ON public.tags
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own tags" ON public.tags
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own tags" ON public.tags
    FOR DELETE USING (auth.uid() = user_id);

-- Create RLS policies for transactions (if not exists)
CREATE POLICY "Users can view own transactions" ON public.transactions
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own transactions" ON public.transactions
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own transactions" ON public.transactions
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own transactions" ON public.transactions
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================================
-- NOTES:
-- ============================================================================
-- 1. Run this migration in Supabase SQL Editor
-- 2. After migration, all queries MUST include user_id filter
-- 3. RLS policies ensure data isolation at database level
-- 4. For existing data, set user_id before enabling RLS policies
