-- create_activities

CREATE ZMVIEW streampay_activities AS
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentSent' AS eventName,
      encode(sc.zilla_identity, 'escape') AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      -sc.amount AS amount,
      CAST(extract(epoch FROM sc.zilla_timestamp) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_commands AS sc
      LEFT JOIN streampay_users fu ON encode(sc.zilla_identity, 'escape') = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.type = 'SendPayment'
  UNION ALL
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentReceived' AS eventName,
      encode(sc.zilla_identity, 'escape') AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      sc.amount AS amount,
      CAST(extract(epoch FROM sc.zilla_timestamp) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_commands AS sc
      LEFT JOIN streampay_users fu ON encode(sc.zilla_identity, 'escape') = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.type = 'SendPayment'
  UNION ALL
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentRequested' AS eventName,
      encode(sc.zilla_identity, 'escape') AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      sc.amount,
      CAST(extract(epoch FROM sc.zilla_timestamp) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_commands sc
      LEFT JOIN streampay_users fu ON encode(sc.zilla_identity, 'escape') = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.type = 'RequestPayment';
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentRejected' AS eventName,
      encode(sc.zilla_identity, 'escape') AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      sc.amount,
      CAST(extract(epoch FROM sc.zilla_timestamp) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_commands sc
      LEFT JOIN streampay_users fu ON encode(sc.zilla_identity, 'escape') = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.type = 'RejectRequest';
