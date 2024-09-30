#!/bin/bash

# Check if a user ID is provided
if [ -z "$1" ]; then
  echo "Usage: $0 <user_id>"
  exit 1
fi

USER_ID="$1"

# Obtain the access token from Keycloak
ACCESS_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/zillabase/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=streampay" \
  -d "username=accountant" \
  -d "password=Test@123" | jq -r .access_token)

# Check if the access token was retrieved successfully
if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ]; then
  echo "Error: Failed to obtain access token."
  exit 1
fi

# Make the curl request with the provided user ID
curl --location 'http://localhost:8080/streampay_initial_balances' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --header "Idempotency-Key: $USER_ID" \
  --header "Authorization: Bearer ${ACCESS_TOKEN}" \
  --data "{
      \"user_id\": \"${USER_ID}\",
      \"initial_balance\": 10000
    }" -v
