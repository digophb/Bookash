-- Migration: Add user_id isolation for multi-tenancy
-- Date: 2026-02-28
-- Priority: P0 - CRITICAL
-- Purpose: Ensure complete data isolation between users

-- =============================================================================
-- STEP 1: Add user_id to categories (if not exists)
-- =============================================================================

-- Check if user_id column exists, if not add it
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'categories' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE public.categories ADD COLUMN user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =============================================================================
-- STEP 2: Create indexes for performance
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_categories_user_id ON public.categories(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON public.accounts(user_id);

-- =============================================================================
-- STEP 3: Handle existing data (optional - choose one strategy)
-- =============================================================================

-- STRATEGY A: Keep existing data as "default/system" categories (user_id = NULL)
-- No action needed - existing categories will be visible to all users
-- New categories will require user_id

-- STRATEGY B: Assign existing data to a specific user (uncomment and modify)
-- UPDATE public.categories SET user_id = '<SPECIFIC_USER_UUID>' WHERE user_id IS NULL;
-- UPDATE public.accounts SET user_id = '<SPECIFIC_USER_UUID>' WHERE user_id IS NULL;

-- =============================================================================
-- STEP 4: Add unique constraints
-- =============================================================================

-- Allow same category name per user (but not globally)
-- Drop existing constraint if any, then add new one
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'unique_user_category_name'
    ) THEN
        ALTER TABLE public.categories DROP CONSTRAINT unique_user_category_name;
    END IF;
END $$;

-- Add partial unique constraint (only for non-null user_id)
CREATE UNIQUE INDEX IF NOT EXISTS unique_user_category_name
    ON public.categories(user_id, name, type)
    WHERE user_id IS NOT NULL;

-- Allow same account name per user
CREATE UNIQUE INDEX IF NOT EXISTS unique_user_account_name
    ON public.accounts(user_id, name)
    WHERE user_id IS NOT NULL;

-- =============================================================================
-- STEP 5: Comments for documentation
-- =============================================================================

COMMENT ON COLUMN public.categories.user_id IS 'Owner user ID. NULL = system/default category visible to all users';
COMMENT ON COLUMN public.accounts.user_id IS 'Owner user ID. Required for user-created accounts';

-- =============================================================================
-- ROLLBACK (if needed)
-- =============================================================================

-- To rollback:
-- ALTER TABLE public.categories DROP COLUMN IF EXISTS user_id;
-- DROP INDEX IF EXISTS idx_categories_user_id;
-- DROP INDEX IF EXISTS unique_user_category_name;
-- DROP INDEX IF EXISTS unique_user_account_name;
