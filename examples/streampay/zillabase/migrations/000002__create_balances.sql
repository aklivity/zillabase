-- create_balances

CREATE VIEW user_transactions AS
  SELECT
      encode(zilla_identity, 'escape') AS user_id,
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

CREATE ZMVIEW streampay_balances AS
  SELECT
      user_id,
      SUM(net_amount) AS balance
  FROM
      all_user_transactions
  GROUP BY
      user_id;
