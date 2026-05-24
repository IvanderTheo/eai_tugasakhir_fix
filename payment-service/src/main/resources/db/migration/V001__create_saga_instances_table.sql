-- Migration: Create saga_instances table for Payment Service
-- Created for: Saga Instance Tracking, Idempotency, and DLQ Error Recovery
-- Date: 2026-05-24

CREATE TABLE IF NOT EXISTS saga_instances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    saga_id VARCHAR(255) NOT NULL UNIQUE,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    saga_topic VARCHAR(255) NOT NULL,
    saga_status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    payload_json LONGTEXT,
    compensation_status VARCHAR(50),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME,
    
    INDEX idx_saga_id (saga_id),
    INDEX idx_message_id (message_id),
    INDEX idx_created_at (created_at),
    INDEX idx_saga_status (saga_status),
    INDEX idx_compensation_status (compensation_status)
);

-- Create index for querying failed sagas for retry
CREATE INDEX idx_failed_sagas ON saga_instances (saga_status, retry_count) 
WHERE saga_status = 'FAILED';

-- Create index for querying pending compensations
CREATE INDEX idx_pending_compensation ON saga_instances (compensation_status) 
WHERE compensation_status = 'PENDING';
