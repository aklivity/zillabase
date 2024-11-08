-- create_payment_requests

CREATE VIEW request_payments AS
  SELECT
      CASE
          WHEN sc.request_id IS NULL OR sc.request_id = '' THEN generate_unique_id()::varchar
          ELSE sc.request_id
      END AS id,
      encode(sc.zilla_identity, 'escape') AS from_user_id,
      u_from.username AS from_username,
      sc.user_id AS to_user_id,
      u_to.username AS to_username,
      sc.amount,
      sc.notes,
      'pending' AS status
  FROM
      streampay_commands sc
  JOIN
      streampay_users u_to ON sc.user_id = u_to.id
  JOIN
      streampay_users u_from ON encode(sc.zilla_identity, 'escape') = u_from.id
  WHERE
      sc.type = 'RequestPayment';

CREATE VIEW status_updates AS
  SELECT
      sc.request_id AS id,
      CASE
          WHEN sc.type = 'SendPayment' THEN 'paid'
          WHEN sc.type = 'RejectRequest' THEN 'rejected'
      END AS status
  FROM
      streampay_commands sc
  WHERE
      sc.type IN ('SendPayment', 'RejectRequest')
    AND (sc.request_id IS NOT NULL AND sc.request_id <> '');

CREATE ZMVIEW streampay_payment_requests AS
  SELECT
      rp.id,
      rp.from_user_id,
      rp.from_username,
      rp.to_user_id,
      rp.to_username,
      rp.amount,
      rp.notes,
      COALESCE(su.status, rp.status) AS status
  FROM
      request_payments rp
  LEFT JOIN
      status_updates su ON su.id = rp.id;

COMMENT ON COLUMN streampay_payment_requests.to_user_id IS 'identity';
