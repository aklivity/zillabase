-- Migration 4: Create streampay_balances table

-- Table for tracking user balances
CREATE TABLE streampay_balances (
    user_id VARCHAR PRIMARY KEY,
    balance DOUBLE PRECISION
);

-- Function to update a user’s balance
CREATE FUNCTION process_user_balance(user_id VARCHAR, amount DOUBLE PRECISION) RETURNS VOID AS $$
BEGIN
    UPDATE streampay_balances
    SET balance = balance + amount
    WHERE user_id = user_id;
END;
$$ LANGUAGE plpgsql;
