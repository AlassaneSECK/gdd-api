CREATE TABLE IF NOT EXISTS budget_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    budget_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    description VARCHAR(512),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_budget_entries PRIMARY KEY (id),
    CONSTRAINT fk_budget_entries_budget FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE CASCADE
);

CREATE INDEX idx_budget_entries_budget_occurred_at ON budget_entries (budget_id, occurred_at DESC, id DESC);
