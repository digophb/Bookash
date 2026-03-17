-- Migration: Add recurring_id column to transactions table
-- This allows grouping all transactions from the same recurring series
-- Run this in Supabase SQL Editor

-- Add recurring_id column if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'transactions' 
        AND column_name = 'recurring_id'
    ) THEN
        ALTER TABLE transactions ADD COLUMN recurring_id UUID;
        
        -- Add index for faster queries
        CREATE INDEX idx_transactions_recurring_id ON transactions(recurring_id);
        
        RAISE NOTICE 'Column recurring_id added to transactions table';
    ELSE
        RAISE NOTICE 'Column recurring_id already exists in transactions table';
    END IF;
END $$;
