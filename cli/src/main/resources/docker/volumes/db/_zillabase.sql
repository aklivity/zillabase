CREATE USER zillabase;

CREATE SCHEMA zb_catalog AUTHORIZATION postgres;

CREATE TABLE IF NOT EXISTS zb_catalog.zviews(
    name VARCHAR PRIMARY KEY,
    sql VARCHAR
);

CREATE TABLE  IF NOT EXISTS zb_catalog.ztables(
    name VARCHAR PRIMARY KEY,
    sql VARCHAR
);

CREATE TABLE  IF NOT EXISTS zb_catalog.zfunctions(
    name VARCHAR PRIMARY KEY,
    sql VARCHAR
);

CREATE TABLE zb_catalog.schema_version(
    version VARCHAR PRIMARY KEY,
    description VARCHAR,
    script_name VARCHAR,
    checksum VARCHAR,
    applied_on TIMESTAMP
);

CREATE ZVIEW zcatalogs AS
      SELECT name AS source_id FROM "zb_catalog"."zviews"
      UNION ALL
      SELECT name AS source_id FROM "zb_catalog"."ztables";
