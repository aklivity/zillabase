-- Table for logging events related to transactions and requests
CREATE TABLE streampay_events (
    id VARCHAR PRIMARY KEY,
    event_name VARCHAR,
    from_user_id VARCHAR,
    to_user_id VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR,
    timestamp DOUBLE PRECISION
);

-- Helper function to log events in the streampay_events table
CREATE FUNCTION log_event(event_name VARCHAR, from_user_id VARCHAR, to_user_id VARCHAR, amount DOUBLE PRECISION, notes VARCHAR) RETURNS VOID AS $$
BEGIN
    INSERT INTO streampay_events (id, event_name, from_user_id, to_user_id, amount, notes, timestamp)
    VALUES (generate_unique_id(), event_name, from_user_id, to_user_id, amount, notes, EXTRACT(EPOCH FROM NOW()) * 1000);
END;
$$ LANGUAGE plpgsql;

-- View to retrieve transaction history from streampay_events
CREATE ZVIEW streampay_transaction_history AS
SELECT
    event_name,
    from_user_id,
    to_user_id,
    amount,
    notes,
    timestamp
FROM streampay_events;
