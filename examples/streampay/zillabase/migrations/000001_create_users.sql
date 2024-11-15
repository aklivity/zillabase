-- Table for storing user information
CREATE ZTABLE streampay_users (
    id VARCHAR PRIMARY KEY,
    name VARCHAR,
    username VARCHAR,
    initial_balance DOUBLE PRECISION
);

-- Helper function to retrieve a username by user ID (useful for functions that reference `streampay_users`)
CREATE FUNCTION get_username(user_id VARCHAR) RETURNS VARCHAR AS $$
DECLARE
    username VARCHAR;
BEGIN
    SELECT u.username INTO username
    FROM streampay_users u
    WHERE u.id = user_id;
    RETURN username;
END;
$$ LANGUAGE plpgsql;
