-- create_payment_requests

CREATE VIEW request_payments AS
  SELECT
      CASE
          WHEN sc.request_id IS NULL OR sc.request_id = '' THEN generate_unique_id()::varchar
          ELSE sc.request_id
      END AS id,
      sc.owner_id AS from_user_id,
      u_from.username AS from_username,
      sc.user_id AS to_user_id_identity,
      u_to.username AS to_username,
      sc.amount,
      sc.notes,
      'pending' AS status
  FROM
      streampay_events sc
  JOIN
      streampay_users u_to ON sc.user_id = u_to.id
  JOIN
      streampay_users u_from ON sc.owner_id = u_from.id
  WHERE
      sc.event = 'PaymentRequested';

CREATE VIEW status_updates AS
  SELECT
      sc.request_id AS id,
      CASE
          WHEN sc.event = 'PaymentSent' THEN 'paid'
          WHEN sc.event = 'PaymentRejected' THEN 'rejected'
      END AS status
  FROM
      streampay_events sc
  WHERE
      sc.event IN ('PaymentSent', 'PaymentRejected')
    AND (sc.request_id IS NOT NULL AND sc.request_id <> '');

CREATE ZVIEW streampay_payment_requests AS
  SELECT
      rp.id,
      rp.from_user_id,
      rp.from_username,
      rp.to_user_id_identity,
      rp.to_username,
      rp.amount,
      rp.notes,
      COALESCE(su.status, rp.status) AS status
  FROM
      request_payments rp
  LEFT JOIN
      status_updates su ON su.id = rp.id;
