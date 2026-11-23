-- V1: Criação da tabela de contas e carga do plano de contas inicial
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    account_code VARCHAR(30) NOT NULL UNIQUE,
    customer_id UUID,
    account_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    reserved_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reserved_balance_non_negative CHECK (reserved_balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts (customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_type_status ON accounts (account_type, status);

-- Seed das contas internas fundamentais do plano de contas
INSERT INTO accounts (id, account_code, customer_id, account_type, status, current_balance, reserved_balance, version, created_at, updated_at)
VALUES 
    ('00000000-0000-0000-0000-000000000001', '1.1.1.01.0001', NULL, 'ASSET', 'ACTIVE', 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', '4.1.1.01.0001', NULL, 'REVENUE', 'ACTIVE', 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', '5.1.1.01.0001', NULL, 'EXPENSE', 'ACTIVE', 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (account_code) DO NOTHING;
