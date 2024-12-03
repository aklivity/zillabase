-- create_commands_and_replies

-- A stream to track all of the user commands for the application
CREATE STREAM streampay_commands(
    type VARCHAR,
    user_id VARCHAR,
    request_id VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR,
    zilla_correlation_id VARCHAR,
    zilla_identity VARCHAR,
    zilla_timestamp TIMESTAMP
);

CREATE MATERIALIZED VIEW IF NOT EXISTS streampay_replies AS
    SELECT '400' AS status, encode(zilla_correlation_id, 'escape') AS correlation_id from streampay_commands where type NOT IN ('SendPayment', 'RequestPayment', 'RejectRequest')
    UNION
    SELECT '200' AS status,  encode(zilla_correlation_id, 'escape') AS correlation_id from streampay_commands where type IN ('SendPayment', 'RequestPayment', 'RejectRequest');
