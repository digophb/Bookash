-- Migration: Add is_archived column to accounts table
-- Date: 2026-02-28
-- Purpose: Enable soft-delete/archiving of accounts

ALTER TABLE public.accounts ADD COLUMN IF NOT EXISTS is_archived BOOLEAN DEFAULT FALSE;

-- Create index for archived filtering
CREATE INDEX IF NOT EXISTS idx_accounts_is_archived ON public.accounts(is_archived);
