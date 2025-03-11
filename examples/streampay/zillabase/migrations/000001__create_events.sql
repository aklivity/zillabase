-- create_events

CREATE TABLE streampay_events (
    correlation_id VARCHAR PRIMARY KEY,
    owner_id VARCHAR,
    created_at TIMESTAMPTZ,
    event VARCHAR,
    user_id VARCHAR,
    request_id VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR
);
