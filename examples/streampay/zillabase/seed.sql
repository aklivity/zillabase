-- seed

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
