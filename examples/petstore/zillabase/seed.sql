-- seed

CREATE TABLE petstore_pets(
  id VARCHAR,
  breed VARCHAR,
  PRIMARY KEY (id)
);

CREATE TABLE petstore_customers(
  name VARCHAR,
  status VARCHAR,
  PRIMARY KEY (name)
);

CREATE TABLE petstore_verified_customers(
  id VARCHAR,
  points VARCHAR,
  PRIMARY KEY (id)
);
