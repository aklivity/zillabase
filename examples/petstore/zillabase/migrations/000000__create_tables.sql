-- create_tables

CREATE ZTABLE petstore_pets(
  id VARCHAR,
  breed VARCHAR,
  PRIMARY KEY (id)
);

CREATE ZTABLE petstore_customers(
  name VARCHAR,
  status VARCHAR,
  PRIMARY KEY (name)
);

CREATE ZTABLE petstore_verified_customers(
  id VARCHAR,
  points VARCHAR,
  PRIMARY KEY (id)
);
