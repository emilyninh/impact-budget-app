-- Records events that exhausted retries and were routed to a .DLT topic, so failures are
-- visible and replayable (POST /api/v1/admin/dlq/replay) instead of being lost in a topic.
CREATE TABLE dead_letter (
    id             UUID PRIMARY KEY,
    dlt_topic      VARCHAR(255) NOT NULL,
    original_topic VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,   -- FQCN of the payload, for replay deserialization
    msg_key        VARCHAR(255),
    payload        TEXT         NOT NULL,
    error_message  TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    replayed_at    TIMESTAMPTZ
);

CREATE INDEX idx_dead_letter_unreplayed ON dead_letter (created_at) WHERE replayed_at IS NULL;
