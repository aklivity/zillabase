CREATE TABLE IF NOT EXISTS users(
   *,
   PRIMARY KEY (user_id)
)
INCLUDE KEY AS user_id
WITH (
    connector='kafka',
    topic='streampay-users',
    properties.bootstrap.server='kafka:29092',
    scan.startup.mode='latest',
    scan.startup.timestamp.millis='140000000'
) FORMAT UPSERT ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);

CREATE TABLE IF NOT EXISTS balances(
    *,
    PRIMARY KEY (user_id)
)
INCLUDE KEY AS user_id
WITH (
    connector='kafka',
    topic='streampay-balances',
    properties.bootstrap.server='kafka:29092',
    scan.startup.mode='latest',
    scan.startup.timestamp.millis='140000000'
) FORMAT UPSERT ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);

CREATE SOURCE IF NOT EXISTS commands
INCLUDE KEY AS key
INCLUDE header'zilla:correlation-id' AS correlation_id
INCLUDE header 'zilla:identity' AS owner_id
INCLUDE timestamp as timestamp
WITH (
    connector='kafka',
    topic='streampay-commands',
    properties.bootstrap.server='kafka:29092',
    scan.startup.mode='latest',
    scan.startup.timestamp.millis='140000000'
) FORMAT PLAIN ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);

CREATE TABLE streampay-commands(
    type VARCHAR,
    userid VARCHAR,
    requestid VARCHAR,
    amount DOUBLE PRECISION,
    notes VARCHAR
)
INCLUDE zilla_correlation_id AS correlation_id
INCLUDE zilla_identity AS ownerid
INCLUDE timestamp as timestamp;

CREATE TABLE streampay-replies(
    status VARCHAR,
    correlationid VARCHAR
);

CREATE TABLE streampay-payment-requests(
  id VARCHAR,
  fromUserId VARCHAR,
  fromUserName VARCHAR,
  toUserId VARCHAR,
  toUserName VARCHAR,
  amount DOUBLE PRECISION,
  notes VARCHAR,
  timestamp LONG
);

 CREATE TABLE streampay-users(
  id VARCHAR primary_key,
  name VARCHAR,
  username VARCHAR
 );

CREATE TABLE streampay-balance(
    user_id VARCHAR primary_key,
    balance DOUBLE PRECISION,
    timestamp LONG
);

CREATE TABLE streampay-balance-histories(
    balance DOUBLE PRECISION,
    timestamp LONG
);

CREATE FUNCTION column(name varchar) RETURNS VARCHAR LANGUAGE javascript AS $$
    return name;
$$;

CREATE FUNCTION generate_guid() RETURNS VARCHAR LANGUAGE javascript AS $$
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

CREATE MATERIALIZED VIEW IF NOT EXISTS invalid_commands AS
    SELECT column("400") as status, encode(correlation_id, 'escape') as correlation_id from commands where key IS NULL OR type NOT IN ('SendPayment', 'RequestPayment');


CREATE MATERIALIZED VIEW IF NOT EXISTS valid_commands AS
    SELECT column("200") as status,  encode(correlation_id, 'escape') as correlation_id from commands where key IS NOT NULL AND type IN ('SendPayment', 'RequestPayment');

CREATE MATERIALIZED VIEW withdrawals_transactions as
SELECT
    generate_guid() as id,
    cmd.user_id as user_id,
    -(cmd.amount) as amount,
    cmd.timestamp as timestamp
FROM
    (
        SELECT
            owner_id::varchar as owner_id,
            user_id,
            amount,
            timestamp
        FROM
            commands
        WHERE
        KEY IS NOT NULL
        AND type = 'SendPayment'
    ) as cmd
    LEFT JOIN (
        SELECT
            user_id::varchar as user_id,
            balance
        FROM
            balances
    ) AS ub ON cmd.owner_id = ub.user_id AND ub.balance >= cmd.amount;


CREATE MATERIALIZED VIEW deposit_transactions as
SELECT
    generate_guid() as id,
    cmd.user_id as user_id,
    +(cmd.amount) as amount,
    cmd.timestamp as timestamp
FROM
    (
        SELECT
            owner_id::varchar as owner_id,
            user_id as user_id,
            amount,
            timestamp
        FROM
            commands
        WHERE
        KEY IS NOT NULL
        AND type = 'SendPayment'
    ) as cmd
    LEFT JOIN (
        SELECT
            user_id::varchar as user_id,
            balance
        FROM
            balances
    ) AS ub ON cmd.owner_id = ub.user_id AND ub.balance >= cmd.amount;


CREATE MATERIALIZED VIEW request_payments as
SELECT
    generate_guid() as id,
    encode(cmd.owner_id, 'escape') as from_user_id,
    u2.username as from_username,
    cmd.user_id as to_user_id,
    u1.username as to_username,
    amount,
    notes
FROM
    commands as cmd
JOIN
    users u1 ON u1.id = cmd.user_id
JOIN
    users u2 ON u2.id = encode(cmd.owner_id, 'escape')
WHERE
    key IS NOT NULL
    AND type = 'RequestPayment';



CREATE MATERIALIZED VIEW payment_sent_events
SELECT
    column("") as name,
    t.user_id AS from_user_id,
    t.amount,
    CURRENT_TIMESTAMP AS timestamp,
    t.owner_id AS to_user_id,
    u.name AS from_user_name
FROM
    withdrawals_transactions t
LEFT JOIN
    users u ON t.user_id = u.id


CREATE SINK invalid_replies
FROM invalid_commands
WITH (
    connector='kafka',
    topic='streampay-replies',
    properties.bootstrap.server='kafka:29092',
    primary_key='correlation_id'
) FORMAT UPSERT ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);

CREATE SINK valid_replies
FROM valid_commands
WITH (
    connector='kafka',
    topic='streampay-replies',
    properties.bootstrap.server='kafka:29092',
    primary_key='correlation_id'
) FORMAT UPSERT ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);

CREATE SINK request_payment_sink
FROM request_payments
WITH (
    connector='kafka',
    topic='streampay-request-payments',
    properties.bootstrap.server='kafka:29092',
    primary_key='id'
) FORMAT UPSERT ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);

CREATE SINK user_balances_sink
FROM user_balances
WITH (
    connector='kafka',
    topic='streampay-balances',
    properties.bootstrap.server='kafka:29092',
    primary_key='id'
) FORMAT UPSERT ENCODE AVRO (
    schema.registry = 'http://schema-registry:8081'
);

