-- seed

/*
=== SQL Functions ===
Define functions that can be used in queries.
*/

-- Local function
CREATE FUNCTION generate_unique_id() RETURNS VARCHAR LANGUAGE javascript AS $$
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = (Math.random() * 16) | 0,
        v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
$$;

-- Remote User Defined Function
CREATE FUNCTION assess_fraud(varchar, varchar, double precision) RETURNS struct<summary varchar, risk varchar>
LANGUAGE python AS assess_fraud;

CREATE FUNCTION process_embedding(varchar, varchar, double precision, varchar) RETURNS boolean
LANGUAGE python AS process_embedding;


/*
=== Tables & Stream Data plane ===
A Table creates the topic and CRUD APIs to insert and query data.
A Stream creates the topic and CRUD APIs to produce and fetch data.
*/

-- A table to store user data
CREATE TABLE streampay_users(
  id VARCHAR,
  name VARCHAR,
  username VARCHAR,
  initial_balance DOUBLE PRECISION,
  PRIMARY KEY (id)
);

-- add initial user data
INSERT INTO streampay_users (id, name, username, initial_balance) VALUES ('allen', 'Allen Doe', 'allen', 10000);
INSERT INTO streampay_users (id, name, username, initial_balance) VALUES ('bertollo', 'Bertollo Doe', 'bertollo', 10000);
INSERT INTO streampay_users (id, name, username, initial_balance) VALUES ('candice', 'Candice Doe', 'candice', 10000);
INSERT INTO streampay_users (id, name, username, initial_balance) VALUES ('dennis', 'Dennis Doe', 'dennis', 10000);
INSERT INTO streampay_users (id, name, username, initial_balance) VALUES ('elaine', 'Elaine Doe', 'elaine', 10000);
INSERT INTO streampay_users (id, name, username, initial_balance) VALUES ('fred', 'Fred Doe', 'fred', 10000);
INSERT INTO streampay_users (id, name, username, initial_balance) VALUES ('greg', 'Greg Doe', 'greg', 10000);

/*
=== Streamed Command messages ===
*/

-- A stream to track all of the user Commands for the system
CREATE STREAM streampay_commands(
    type VARCHAR,
    user_id VARCHAR,
    request_id VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR
)
INCLUDE zilla_correlation_id AS correlation_id
INCLUDE zilla_identity AS owner_id
INCLUDE timestamp AS timestamp;

CREATE MATERIALIZED VIEW IF NOT EXISTS streampay_replies AS
    SELECT '400' AS status, encode(correlation_id, 'escape') AS correlation_id from streampay_commands where type NOT IN ('SendPayment', 'RequestPayment', 'RejectRequest')
    UNION
    SELECT '200' AS status,  encode(correlation_id, 'escape') AS correlation_id from streampay_commands where type IN ('SendPayment', 'RequestPayment', 'RejectRequest');

/*
=== APIs ===
*/

CREATE STREAM streampay_balance_histories(
    balance DOUBLE PRECISION
)
INCLUDE timestamp AS timestamp;

CREATE VIEW user_transactions AS
  SELECT
      encode(owner_id, 'escape') AS user_id,
      -amount AS net_amount
  FROM streampay_commands
  WHERE type = 'SendPayment'
  UNION ALL
  SELECT
      user_id AS user_id,
      amount AS net_amount
  FROM streampay_commands
  WHERE type = 'SendPayment';

CREATE VIEW all_user_transactions AS
  SELECT
      id as user_id,
      initial_balance AS net_amount
  FROM
      streampay_users
  UNION ALL
  SELECT
      user_id,
      net_amount
  FROM
      user_transactions;

CREATE MATERIALIZED VIEW streampay_balances AS
  SELECT
      user_id,
      SUM(net_amount) AS balance
  FROM
      all_user_transactions
  GROUP BY
      user_id;

CREATE MATERIALIZED VIEW streampay_payment_requests AS
  SELECT
      CASE
          WHEN sc.type = 'RequestPayment' THEN generate_unique_id()::varchar
          ELSE sc.request_id
      END AS id,
      encode(sc.owner_id, 'escape') AS from_user_id,
      u2.username AS from_username,
      sc.user_id AS to_user_id_identity,
      u1.username AS to_username,
      sc.amount,
      sc.notes,
      CASE
          WHEN sc.type = 'RequestPayment' THEN 'pending'
          WHEN sc.type = 'SendPayment' THEN 'paid'
          WHEN sc.type = 'RejectRequest' THEN 'rejected'
      END AS status
  FROM
      streampay_commands sc
  JOIN
      streampay_users u1 ON sc.user_id = u1.id
  JOIN
      streampay_users u2 ON encode(sc.owner_id, 'escape') = u2.id
  WHERE
      sc.type IN ('RequestPayment', 'SendPayment', 'RejectRequest');

CREATE MATERIALIZED VIEW streampay_payment_risk_assessment AS
  SELECT
      ar.id,
      ar.to_user_id_identity,
      (ar.fraud).summary AS summary,
      (ar.fraud).risk AS risk
  FROM (
    SELECT
      *,
      assess_fraud(from_username, to_username, amount) AS fraud
    FROM
      streampay_payment_requests
  ) AS ar;

CREATE MATERIALIZED VIEW streampay_activities AS
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentSent' AS eventName,
      encode(sc.owner_id, 'escape') AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      -sc.amount AS amount,
      CAST(extract(epoch FROM sc.timestamp) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_commands AS sc
      LEFT JOIN streampay_users fu ON encode(sc.owner_id, 'escape') = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.type = 'SendPayment'
  UNION ALL
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentReceived' AS eventName,
      encode(sc.owner_id, 'escape') AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      sc.amount AS amount,
      CAST(extract(epoch FROM sc.timestamp) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_commands AS sc
      LEFT JOIN streampay_users fu ON encode(sc.owner_id, 'escape') = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.type = 'SendPayment'
  UNION ALL
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentRequested' AS eventName,
      encode(sc.owner_id, 'escape') AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      sc.amount,
      CAST(extract(epoch FROM sc.timestamp) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_commands sc
      LEFT JOIN streampay_users fu ON encode(sc.owner_id, 'escape') = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.type = 'RequestPayment';
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentRejected' AS eventName,
      encode(sc.owner_id, 'escape') AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      sc.amount,
      CAST(extract(epoch FROM sc.timestamp) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_commands sc
      LEFT JOIN streampay_users fu ON encode(sc.owner_id, 'escape') = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.type = 'RejectRequest';

CREATE VIEW streampay_payment_process_embedding AS
  SELECT
      id,
      process_embedding(from_username, to_username, amount, eventName) AS result
  FROM
      streampay_activities
  WHERE eventName IN ('PaymentReceived', 'PaymentRejected');
