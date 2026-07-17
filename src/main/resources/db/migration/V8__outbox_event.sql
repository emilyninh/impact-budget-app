-- Transactional outbox: domain writes and their "event to publish" are committed in one
-- transaction (this table), then a relay publishes unsent rows to Kafka and stamps
-- published_at. This removes the event-loss window between a DB commit and a best-effort
-- KafkaTemplate.send. Consumers are idempotent, so at-least-once redelivery is safe.
CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,   -- FQCN of the event payload, for deserialization
    topic          VARCHAR(255) NOT NULL,
    msg_key        VARCHAR(255),
    payload        TEXT         NOT NULL,    -- event serialized as JSON
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

-- The relay only ever scans unpublished rows; a partial index keeps that cheap as the
-- table grows with published history.
CREATE INDEX idx_outbox_unpublished ON outbox_event (created_at) WHERE published_at IS NULL;
