-- Table for storing payment requests with status tracking
CREATE ZTABLE streampay_payment_requests (
    id VARCHAR PRIMARY KEY,
    from_user_id VARCHAR,
    from_username VARCHAR,
    to_user_id VARCHAR,
    to_username VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR,
    status VARCHAR
);

-- Function to generate or return a request ID (used in request management)
CREATE FUNCTION generate_request_id(request_id VARCHAR) RETURNS VARCHAR AS $$
BEGIN
    RETURN COALESCE(request_id, generate_unique_id());
END;
$$ LANGUAGE plpgsql;

-- Function to create or update a payment request
CREATE FUNCTION create_or_update_request(command RECORD) RETURNS VOID AS $$
DECLARE
    request_id VARCHAR := generate_request_id(command.request_id);
    from_user_id VARCHAR := encode(command.zilla_identity, 'escape');
    from_username VARCHAR := get_username(from_user_id);
    to_user_id VARCHAR := command.user_id;
    to_username VARCHAR := get_username(to_user_id);
BEGIN
    INSERT INTO streampay_payment_requests (
        id, from_user_id, from_username, to_user_id, to_username, amount, notes, status
    ) VALUES (
        request_id,
        from_user_id,
        from_username,
        to_user_id,
        to_username,
        command.amount,
        command.notes,
        'pending'
    )
    ON CONFLICT (id) DO UPDATE
    SET amount = EXCLUDED.amount,
        notes = EXCLUDED.notes,
        status = 'pending';
END;
$$ LANGUAGE plpgsql;

