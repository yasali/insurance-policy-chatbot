-- liquibase formatted sql
-- changeset codetest:1-initial-migration.sql

-- Insurance table
CREATE TABLE insurance (
    id UUID PRIMARY KEY,
    personal_number VARCHAR(12) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_personal_number UNIQUE (personal_number)
);

CREATE INDEX idx_insurance_personal_number ON insurance(personal_number);

-- Policy table 
CREATE TABLE policy (
    id UUID PRIMARY KEY,
    insurance_id UUID NOT NULL,
    address VARCHAR(255) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_insurance FOREIGN KEY (insurance_id) REFERENCES insurance(id) ON DELETE CASCADE,
    CONSTRAINT check_date_order CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_policy_insurance_id ON policy(insurance_id);
CREATE INDEX idx_policy_start_date ON policy(start_date);
CREATE INDEX idx_policy_date_range ON policy(start_date, end_date);

-- Conversation table
CREATE TABLE conversation (
    id UUID PRIMARY KEY,
    insurance_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation_insurance FOREIGN KEY (insurance_id) REFERENCES insurance(id) ON DELETE CASCADE
);

CREATE INDEX idx_conversation_insurance_id ON conversation(insurance_id);

-- Conversation messages
CREATE TABLE conversation_message (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL, -- SYSTEM, USER, ASSISTANT
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    CONSTRAINT check_valid_role CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT'))
);

CREATE INDEX idx_message_conversation_id ON conversation_message(conversation_id);
CREATE INDEX idx_message_created_at ON conversation_message(created_at);

-- Claim table
CREATE TABLE claim (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL,
    raw_text TEXT NOT NULL,
    extracted_data JSON,
    confidence DOUBLE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claim_policy FOREIGN KEY (policy_id) REFERENCES policy(id) ON DELETE CASCADE,
    CONSTRAINT check_valid_status CHECK (status IN ('PENDING', 'EXTRACTED', 'VALIDATED', 'REJECTED')),
    CONSTRAINT check_confidence_range CHECK (confidence IS NULL OR (confidence >= 0.0 AND confidence <= 1.0))
);

CREATE INDEX idx_claim_policy_id ON claim(policy_id);
CREATE INDEX idx_claim_status ON claim(status);
CREATE INDEX idx_claim_created_at ON claim(created_at);
