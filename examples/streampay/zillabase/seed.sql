-- seed

CREATE FUNCTION column_value(name varchar) RETURNS TABLE (value varchar) LANGUAGE javascript AS $$
    yield i;
$$;

CREATE FUNCTION generate_guid() RETURNS TABLE (value varchar) LANGUAGE javascript AS $$
    var result, i, j;
    result = '';
    for(j=0; j<32; j++) {
        if( j == 8 || j == 12 || j == 16 || j == 20)
          result = result + '-';
        i = Math.floor(Math.random()*16).toString(16).toUpperCase();
        result = result + i;
    }
  return result;
$$;

CREATE TABLE streampay_commands(
    type VARCHAR,
    userid VARCHAR,
    requestid VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR
)
INCLUDE zilla_correlation_id AS correlation_id
INCLUDE zilla_identity AS owner_id
INCLUDE timestamp as timestamp;

CREATE TABLE streampay_replies(
    status VARCHAR,
    correlationid VARCHAR
);

CREATE TABLE streampay_payment_requests(
  id VARCHAR,
  fromUserId VARCHAR,
  fromUserName VARCHAR,
  toUserId VARCHAR,
  toUserName VARCHAR,
  amount DOUBLE PRECISION,
  notes VARCHAR
)
INCLUDE timestamp AS timestamp;

CREATE TABLE streampay_users(
  id VARCHAR,
  name VARCHAR,
  username VARCHAR,
  PRIMARY KEY (id)
);

CREATE TABLE streampay_balance(
    user_id VARCHAR,
    balance DOUBLE PRECISION,
    PRIMARY KEY (user_id)
)
INCLUDE timestamp AS timestamp;

CREATE TABLE streampay_balance_histories(
    balance DOUBLE PRECISION
)
INCLUDE timestamp AS timestamp;

CREATE MATERIALIZED VIEW IF NOT EXISTS invalid_commands AS
    SELECT column_value('400') as status, encode(correlation_id, 'escape') as correlation_id from streampay_commands where type NOT IN ('SendPayment', 'RequestPayment');

CREATE MATERIALIZED VIEW IF NOT EXISTS valid_commands AS
    SELECT column_value('200') as status,  encode(correlation_id, 'escape') as correlation_id from streampay_commands where NOT NULL AND type IN ('SendPayment', 'RequestPayment');
