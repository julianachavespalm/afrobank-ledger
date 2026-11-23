-- V2: Criação das tabelas imutáveis do livro-razão (Journal Entries e Postings)

CREATE TABLE IF NOT EXISTS journal_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    entry_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    booking_date DATE NOT NULL,
    effective_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),
    reversed_entry_id UUID REFERENCES journal_entries (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_journal_entries_date ON journal_entries (booking_date);
CREATE INDEX IF NOT EXISTS idx_journal_entries_tx ON journal_entries (transaction_id);

CREATE TABLE IF NOT EXISTS postings (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries (id),
    account_id UUID NOT NULL REFERENCES accounts (id),
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_posting_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_posting_direction CHECK (direction IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX IF NOT EXISTS idx_postings_account_time ON postings (account_id, recorded_at);
CREATE INDEX IF NOT EXISTS idx_postings_journal_entry ON postings (journal_entry_id);

-- Função e trigger para garantir imutabilidade estrita (Append-Only)
CREATE OR REPLACE FUNCTION prevent_postings_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Postings are strictly append-only: UPDATE and DELETE operations are prohibited';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_postings_append_only ON postings;
CREATE TRIGGER trg_postings_append_only
BEFORE UPDATE OR DELETE ON postings
FOR EACH ROW
EXECUTE FUNCTION prevent_postings_mutation();
