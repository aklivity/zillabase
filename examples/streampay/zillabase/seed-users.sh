#!/bin/bash

JOHN_ACCESS_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/zillabase/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=streampay" \
  -d "username=johndoe" \
  -d "password=Test@123" | jq -r .access_token)

JOHN_USER_INFO=$(curl -s -X GET "http://localhost:8180/realms/zillabase/account" \
  -H "Authorization: Bearer ${JOHN_ACCESS_TOKEN}" \
  -H "Content-Type: application/json")

JOHN_USER_ID=$(echo "$JOHN_USER_INFO" | jq -r '.id')

curl --location 'http://localhost:8080/streampay_users' \
--header "Content-Type: application/json" \
--header "Accept: application/json" \
--header "Idempotency-Key: ${JOHN_USER_ID}" \
--header "Authorization: Bearer ${JOHN_ACCESS_TOKEN}" \
--data "{
  \"id\": \"${JOHN_USER_ID}\",
  \"name\": \"John Doe\",
  \"username\": \johndoe\"
}"

curl --location -X POST 'http://localhost:8080/streampay_initial_balances' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --header "Idempotency-Key: ${JOHN_USER_ID}" \
  --header "Authorization: Bearer ${JOHN_ACCESS_TOKEN}" \
  --data "{
      \"user_id\": \"${JOHN_USER_ID}\",
      \"initial_balance\": 10000
    }"

JANE_ACCESS_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/zillabase/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=streampay" \
  -d "username=janedoe" \
  -d "password=Test@123" | jq -r .access_token)

JANE_USER_INFO=$(curl -s -X GET "http://localhost:8180/realms/zillabase/account" \
  -H "Authorization: Bearer ${JANE_ACCESS_TOKEN}" \
  -H "Content-Type: application/json")

JANE_USER_ID=$(echo "$JANE_USER_INFO" | jq -r '.id')

curl --location 'http://localhost:8080/streampay_users' \
--header "Content-Type: application/json" \
--header "Accept: application/json" \
--header "Idempotency-Key: ${JANE_USER_ID}" \
--header "Authorization: Bearer ${JANE_ACCESS_TOKEN}" \
--data "{
  \"id\": \"${JANE_USER_ID}\",
  \"name\": \"Jane Doe\",
  \"username\": \janedoe\"
}"

curl --location -X POST 'http://localhost:8080/streampay_initial_balances' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --header "Idempotency-Key: ${JANE_USER_ID}" \
  --header "Authorization: Bearer ${JANE_ACCESS_TOKEN}" \
  --data "{
      \"user_id\": \"${JOHN_USER_ID}\",
      \"initial_balance\": 10000
    }"
