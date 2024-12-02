-- Core function for processing commands with modular handling
CREATE OR REPLACE FUNCTION compute_status(command RECORD) RETURNS RECORD AS $$
DECLARE
    reply RECORD;
BEGIN
    CASE command.type
        WHEN 'SendPayment' THEN
            IF (SELECT balance FROM streampay_balances WHERE user_id = command.user_id) >= command.amount THEN
                adjust_user_balance(command.user_id, -command.amount);
                adjust_user_balance(command.request_id, command.amount);
                reply.status := '200';  -- Success
                log_event('PaymentSent', command.user_id, command.request_id, command.amount, command.notes);
            ELSE
                reply.status := '402';  -- Insufficient funds
                log_event('PaymentFailed', command.user_id, command.request_id, command.amount, command.notes);
            END IF;
        WHEN 'RequestPayment' THEN
            create_or_update_request(command);
            reply.status := '202';  -- Pending
        WHEN 'RejectRequest' THEN
            process_status_update(command);
            reply.status := '200';  -- Success
        ELSE
            reply.status := '400';  -- Bad Request
    END CASE;

    RETURN reply;
END;
$$ LANGUAGE plpgsql;

-- Function to process status updates such as 'paid' or 'rejected'
CREATE FUNCTION process_status_update(command RECORD) RETURNS VOID AS $$
BEGIN
    IF command.type = 'SendPayment' THEN
        update_request_status(command.request_id, 'paid');
        log_event('PaymentPaid', command.user_id, command.request_id, command.amount, command.notes);
    ELSIF command.type = 'RejectRequest' THEN
        update_request_status(command.request_id, 'rejected');
        log_event('PaymentRejected', command.user_id, command.request_id, command.amount, command.notes);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Stream to handle commands using compute_status as the processing function
CREATE ZSTREAM streampay_commands (
    type VARCHAR,
    user_id VARCHAR,
    request_id VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR
) WITH (
    reply_to = 'streampay_replies',
    function = compute_status,
    timestamp = 'timestamp',
    identity = 'owner_id'
);
