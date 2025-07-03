-- Fix existing records with null enabled values
UPDATE app_user SET enabled = false WHERE enabled IS NULL;

-- Also ensure locked field is not null
UPDATE app_user SET locked = false WHERE locked IS NULL;

-- Verify the fix
SELECT email, enabled, locked FROM app_user; 