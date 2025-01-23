-- create_handlers

CREATE ZFUNCTION streampay_send_payment(
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
$$
WITH(
    EVENTS = 'streampay_events'
);

CREATE ZFUNCTION streampay_request_payment(
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
$$
WITH(
    EVENTS = 'streampay_events'
);

CREATE ZFUNCTION streampay_reject_payment(
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
$$
WITH(
    EVENTS = 'streampay_events'
);

