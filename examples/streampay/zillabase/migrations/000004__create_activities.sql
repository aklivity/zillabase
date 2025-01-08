-- create_activities

CREATE ZVIEW streampay_activities AS
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentSent' AS eventName,
      sc.owner_id AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      -sc.amount AS amount,
      CAST(extract(epoch FROM sc.created_at) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_events AS sc
      LEFT JOIN streampay_users fu ON sc.owner_id = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.event = 'PaymentSent'
  UNION ALL
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentReceived' AS eventName,
      sc.user_id AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      sc.amount AS amount,
      CAST(extract(epoch FROM sc.created_at) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_events AS sc
      LEFT JOIN streampay_users fu ON sc.owner_id = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.event = 'PaymentSent'
  UNION ALL
  SELECT
      generate_unique_id()::varchar AS id,
      'PaymentRequested' AS eventName,
      sc.owner_id AS from_user_id,
      fu.username AS from_username,
      sc.user_id AS to_user_id,
      tu.username AS to_username,
      sc.amount,
      CAST(extract(epoch FROM sc.created_at) AS FLOAT) * 1000 AS timestamp
  FROM
      streampay_events sc
      LEFT JOIN streampay_users fu ON sc.owner_id = fu.id
      LEFT JOIN streampay_users tu ON sc.user_id = tu.id
  WHERE
      sc.event = 'PaymentRequested';
