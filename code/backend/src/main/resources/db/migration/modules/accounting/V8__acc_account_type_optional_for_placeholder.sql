-- Placeholder accounts only group other accounts and have no account type
ALTER TABLE ${schema}.acc_account ALTER COLUMN account_type DROP NOT NULL;
