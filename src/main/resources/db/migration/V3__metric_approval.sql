CREATE TABLE IF NOT EXISTS metric_approval_request (
    id BIGSERIAL PRIMARY KEY,
    metric_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_by VARCHAR(100),
    reviewed_by VARCHAR(100),
    review_comment VARCHAR(500),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_topic_status ON metric_approval_request (topic_id, status);
CREATE INDEX IF NOT EXISTS idx_approval_metric_status ON metric_approval_request (metric_id, status);
