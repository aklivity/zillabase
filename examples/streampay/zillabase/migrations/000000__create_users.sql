-- create_users

/*
=== Tables & Stream Data plane ===
A Table creates the topic and CRUD APIs to insert and query data.
A Stream creates the topic and CRUD APIs to produce and fetch data.
*/


-- A table to store user data
CREATE ZTABLE streampay_users(
  id VARCHAR,
  name VARCHAR,
  username VARCHAR,
  initial_balance DOUBLE PRECISION,
  PRIMARY KEY (id)
);
