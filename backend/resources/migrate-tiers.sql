-- Migrate old FREE tier to BASIC (FREE was renamed to BASIC)
UPDATE USERS SET TIER = 'BASIC' WHERE TIER = 'FREE';
