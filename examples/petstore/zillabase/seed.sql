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

INSERT INTO petstore_pets (id, breed) VALUES ('123', 'German Shepherd');
INSERT INTO petstore_pets (id, breed) VALUES ('234', 'Beagle');

INSERT INTO petstore_customers (name, status) VALUES ('John', 'Active');
INSERT INTO petstore_customers (name, status) VALUES ('Jane', 'Inactive');

INSERT INTO petstore_verified_customers (id, points) VALUES ('John', '100');
