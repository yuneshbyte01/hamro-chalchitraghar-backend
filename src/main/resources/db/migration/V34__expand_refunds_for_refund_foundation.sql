ALTER TABLE refunds ADD COLUMN booking_id BIGINT;
ALTER TABLE refunds ADD COLUMN currency VARCHAR(10);
ALTER TABLE refunds ADD COLUMN reason VARCHAR(50);
ALTER TABLE refunds ADD COLUMN refund_type VARCHAR(30);
ALTER TABLE refunds ADD COLUMN refund_method VARCHAR(30);
ALTER TABLE refunds ADD COLUMN idempotency_key VARCHAR(200);
ALTER TABLE refunds ADD COLUMN requested_by_user_id BIGINT;
ALTER TABLE refunds ADD COLUMN approved_by_user_id BIGINT;
ALTER TABLE refunds ADD COLUMN rejected_by_user_id BIGINT;
ALTER TABLE refunds ADD COLUMN provider VARCHAR(30);
ALTER TABLE refunds RENAME COLUMN provider_refund_id TO provider_refund_reference;
ALTER TABLE refunds ADD COLUMN failure_reason VARCHAR(500);
ALTER TABLE refunds ADD COLUMN requested_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN approved_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN rejected_at TIMESTAMP;

UPDATE refunds r
SET booking_id = p.booking_id,
    currency = p.currency,
    reason = 'OTHER',
    refund_type = 'FULL',
    refund_method = 'MANUAL',
    idempotency_key = 'LEGACY:' || r.refund_reference,
    provider = p.provider,
    requested_at = r.created_at
FROM payments p
WHERE p.id = r.payment_id;

UPDATE refunds SET status = CASE status
    WHEN 'CREATED' THEN 'REQUESTED'
    WHEN 'PENDING' THEN 'REQUESTED'
    WHEN 'SUCCESS' THEN 'SUCCEEDED'
    WHEN 'CANCELLED' THEN 'REJECTED'
    ELSE status
END;

ALTER TABLE refunds ALTER COLUMN booking_id SET NOT NULL;
ALTER TABLE refunds ALTER COLUMN currency SET NOT NULL;
ALTER TABLE refunds ALTER COLUMN reason SET NOT NULL;
ALTER TABLE refunds ALTER COLUMN refund_type SET NOT NULL;
ALTER TABLE refunds ALTER COLUMN refund_method SET NOT NULL;
ALTER TABLE refunds ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE refunds ALTER COLUMN requested_at SET NOT NULL;

ALTER TABLE refunds ADD CONSTRAINT fk_refunds_booking FOREIGN KEY (booking_id) REFERENCES bookings(id);
ALTER TABLE refunds ADD CONSTRAINT fk_refunds_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users(id);
ALTER TABLE refunds ADD CONSTRAINT fk_refunds_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES users(id);
ALTER TABLE refunds ADD CONSTRAINT fk_refunds_rejected_by FOREIGN KEY (rejected_by_user_id) REFERENCES users(id);
ALTER TABLE refunds ADD CONSTRAINT uk_refunds_idempotency_key UNIQUE (idempotency_key);
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_currency_not_blank CHECK (BTRIM(currency) <> '');
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_status CHECK (status IN ('REQUESTED','APPROVED','REJECTED','PROCESSING','SUCCEEDED','FAILED','MANUAL_REVIEW'));
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_reason CHECK (reason IN ('CUSTOMER_CANCELLATION','SHOW_CANCELLATION','ADMIN_ADJUSTMENT','LATE_PAYMENT_SUCCESS','DUPLICATE_PAYMENT','OTHER'));
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_type CHECK (refund_type IN ('FULL','PARTIAL'));
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_method CHECK (refund_method IN ('MANUAL','PROVIDER'));

CREATE INDEX idx_refunds_booking ON refunds (booking_id);
CREATE INDEX idx_refunds_status_created ON refunds (status, created_at DESC);
CREATE INDEX idx_refunds_reason_created ON refunds (reason, created_at DESC);
CREATE INDEX idx_refunds_requested_by_created ON refunds (requested_by_user_id, created_at DESC);
CREATE UNIQUE INDEX uk_refunds_provider_reference ON refunds (provider_refund_reference) WHERE provider_refund_reference IS NOT NULL;
