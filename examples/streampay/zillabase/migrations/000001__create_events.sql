-- create_events

CREATE ZFUNCTION send_payment_handler(
   user_id VARCHAR,
   request_id VARCHAR,
   amount DOUBLE PRECISION,
   notes VARCHAR)
RETURNS TABLE(
   event VARCHAR,
   user_id VARCHAR,
   request_id VARCHAR,
   amount DOUBLE PRECISION,
   notes VARCHAR)
LANGUAGE SQL AS $$
  SELECT
      'PaymentSent' AS event,
      args.user_id,
      args.request_id,
      args.amount,
      args.notes;
  $$;

CREATE ZFUNCTION request_payment_handler(
   user_id VARCHAR,
   request_id VARCHAR,
   amount DOUBLE PRECISION,
   notes VARCHAR)
RETURNS TABLE(
   event VARCHAR,
   user_id VARCHAR,
   request_id VARCHAR,
   amount DOUBLE PRECISION,
   notes VARCHAR)
LANGUAGE SQL AS $$
  SELECT
      'PaymentRequested' AS event,
      args.user_id,
      args.request_id,
      args.amount,
      args.notes;
  $$;

CREATE ZFUNCTION reject_payment_handler(
   user_id VARCHAR,
   request_id VARCHAR,
   amount DOUBLE PRECISION,
   notes VARCHAR)
RETURNS TABLE(
   event VARCHAR,
   user_id VARCHAR,
   request_id VARCHAR,
   amount DOUBLE PRECISION,
   notes VARCHAR)
LANGUAGE SQL AS $$
  SELECT
      'PaymentRejected' AS event,
      args.user_id,
      args.request_id,
      args.amount,
      args.notes;
  $$;

FLUSH;

CREATE ZSTREAM streampay_events(
    event VARCHAR,
    user_id VARCHAR,
    request_id VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR,
    owner_id VARCHAR GENERATED ALWAYS AS IDENTITY,
    created_at TIMESTAMP GENERATED ALWAYS AS NOW
)
WITH (
    DISPATCH_ON = 'command',
    HANDLERS = (
        'SendPayment' TO 'send_payment_handler',
        'RequestPayment' TO 'request_payment_handler',
        'RejectPayment' TO 'reject_payment_handler'
    )
);
