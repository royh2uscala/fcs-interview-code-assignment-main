#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
STORE_NAME="STORE-UAT-$(date +%s)"

echo "1) Create Store (committed write should enqueue outbox)"
CREATE_RESPONSE=$(curl -sS -X POST "$BASE_URL/store" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$STORE_NAME\",\"quantityProductsInStock\":9}")
echo "$CREATE_RESPONSE"
STORE_ID=$(echo "$CREATE_RESPONSE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')

echo "2) Check outbox stats"
curl -sS "$BASE_URL/admin/outbox/stats"
echo

echo "3) Publish pending outbox messages"
curl -sS -X POST "$BASE_URL/admin/outbox/publish"
echo

echo "4) Replay outbox by aggregateId over a short time range"
NOW=$(date -u +"%Y-%m-%dT%H:%M:%S")
FROM=$(date -u -v-5M +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -u -d "-5 minutes" +"%Y-%m-%dT%H:%M:%S")
curl -sS -X POST "$BASE_URL/admin/outbox/replay?aggregateId=$STORE_ID&from=$FROM&to=$NOW"
echo

echo "5) Publish replayed messages"
curl -sS -X POST "$BASE_URL/admin/outbox/publish"
echo
