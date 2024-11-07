-- create_commands_and_replies

-- A stream to track all of the user commands for the application
CREATE OR REPLACE FUNCTION compute_status_and_correlation_id(record RECORD)
RETURNS RECORD AS $$
BEGIN
    record.status := CASE
        WHEN record.type IN ('SendPayment', 'RequestPayment', 'RejectRequest') THEN '200'
        ELSE '400'
    END;
    record.correlation_id := encode(record.zilla_correlation_id, 'escape');
    RETURN record;
END;
$$ LANGUAGE plpgsql;

CREATE ZSTREAM streampay_commands (
    type VARCHAR,
    user_id VARCHAR,
    request_id VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR
) WITH (
    reply_to = 'streampay_replies',
    function = compute_status_and_correlation_id,
    timestamp = 'timestamp',
    identity = 'owner_id'
);
