-- create_payment_risk_assessment

-- python user-defined function
CREATE FUNCTION assess_fraud(varchar, varchar, double precision) RETURNS struct<summary varchar, risk varchar>
LANGUAGE python AS assess_fraud;

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

CREATE FUNCTION process_embedding(varchar, varchar, double precision, varchar) RETURNS boolean
LANGUAGE python AS process_embedding;

CREATE VIEW streampay_payment_process_embedding AS
  SELECT
      id,
      process_embedding(from_username, to_username, amount, eventName) AS result
  FROM
      streampay_activities
  WHERE eventName IN ('PaymentReceived', 'PaymentRejected');
