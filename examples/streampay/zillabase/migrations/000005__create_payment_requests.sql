-- create_payment_requests

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
