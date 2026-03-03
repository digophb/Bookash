-- Migration: Create categories table
-- Date: 2026-03-03
-- Priority: P0 - CRITICAL

-- ============================================================================
-- Create categories table
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.categories (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'expense', -- 'income' or 'expense'
    color TEXT DEFAULT '#357266',
    icon TEXT DEFAULT 'category',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Unique constraint: one user can't have duplicate category names per type
    CONSTRAINT unique_user_category_name UNIQUE (user_id, name, type)
);

-- Index for user_id lookups
CREATE INDEX IF NOT EXISTS idx_categories_user_id ON public.categories(user_id);

-- Index for type filtering
CREATE INDEX IF NOT EXISTS idx_categories_type ON public.categories(type);

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON TABLE public.categories IS 'Categories for transactions. Default categories have user_id = NULL. Personal categories have user_id set.';
COMMENT ON COLUMN public.categories.user_id IS 'NULL for default categories, user UUID for personal categories';
COMMENT ON COLUMN public.categories.type IS 'income or expense';
COMMENT ON COLUMN public.categories.icon IS 'Icon identifier string (e.g., salary, food, transport)';
