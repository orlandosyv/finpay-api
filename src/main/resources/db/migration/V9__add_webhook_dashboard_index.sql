CREATE INDEX IX_outbox_events_merchant_created
ON dbo.outbox_events (merchant_id, created_at DESC)
INCLUDE (aggregate_id, event_type, status, attempts, available_at, processed_at);
GO
