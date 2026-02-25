#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BU_CODE="MWH-UAT-$(date +%s)"

log() {
  printf "\n[%s] %s\n" "$(date +%H:%M:%S)" "$1"
}

assert_status() {
  local got="$1"
  local expected="$2"
  local label="$3"
  if [[ "$got" != "$expected" ]]; then
    echo "FAILED: $label (expected $expected, got $got)"
    exit 1
  fi
  echo "OK: $label ($got)"
}

request_json() {
  local method="$1"
  local path="$2"
  local payload="${3:-}"
  local body_file
  body_file="$(mktemp)"
  local status
  if [[ -n "$payload" ]]; then
    status=$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" "$BASE_URL$path" \
      -H "Content-Type: application/json" \
      -d "$payload")
  else
    status=$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" "$BASE_URL$path")
  fi
  local body
  body="$(cat "$body_file")"
  rm -f "$body_file"
  printf "%s\n%s\n" "$status" "$body"
}

log "1) Create Warehouse (happy path)"
CREATE_RESULT=$(request_json "POST" "/warehouse" \
  "{\"businessUnitCode\":\"$BU_CODE\",\"location\":\"EINDHOVEN-001\",\"capacity\":20,\"stock\":10}")
CREATE_STATUS=$(echo "$CREATE_RESULT" | sed -n '1p')
CREATE_BODY=$(echo "$CREATE_RESULT" | sed -n '2,$p')
assert_status "$CREATE_STATUS" "200" "create warehouse"
echo "$CREATE_BODY"
WAREHOUSE_ID=$(echo "$CREATE_BODY" | sed -n 's/.*"id":"\{0,1\}\([0-9]*\)".*/\1/p')

log "2) Get warehouse by ID"
GET_STATUS=$(curl -sS -o /tmp/warehouse_uat_get.json -w "%{http_code}" "$BASE_URL/warehouse/$WAREHOUSE_ID")
assert_status "$GET_STATUS" "200" "get warehouse by id"
cat /tmp/warehouse_uat_get.json
rm -f /tmp/warehouse_uat_get.json

log "3) Validate non-numeric ID is rejected"
BAD_ID_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/warehouse/NOT_NUMERIC")
assert_status "$BAD_ID_STATUS" "400" "invalid id format"

log "4) Replace warehouse (happy path)"
REPLACE_STATUS=$(curl -sS -o /tmp/warehouse_uat_replace.json -w "%{http_code}" \
  -X POST "$BASE_URL/warehouse/$BU_CODE/replacement" \
  -H "Content-Type: application/json" \
  -d '{"location":"AMSTERDAM-002","capacity":30,"stock":10}')
assert_status "$REPLACE_STATUS" "200" "replace warehouse"
cat /tmp/warehouse_uat_replace.json
rm -f /tmp/warehouse_uat_replace.json

log "5) Replacement stock mismatch should fail"
REPLACE_BAD_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/warehouse/$BU_CODE/replacement" \
  -H "Content-Type: application/json" \
  -d '{"location":"AMSTERDAM-001","capacity":40,"stock":9}')
assert_status "$REPLACE_BAD_STATUS" "400" "replacement stock mismatch"

log "6) Invalid location on create should fail"
BAD_LOC_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/warehouse" \
  -H "Content-Type: application/json" \
  -d "{\"businessUnitCode\":\"$BU_CODE-BADLOC\",\"location\":\"INVALID-001\",\"capacity\":20,\"stock\":10}")
assert_status "$BAD_LOC_STATUS" "400" "invalid location create"

log "7) Archive latest active warehouse ID"
LATEST_ID=$(curl -sS "$BASE_URL/warehouse" | sed -n "s/.*\"businessUnitCode\":\"$BU_CODE\"[^}]*\"id\":\"\{0,1\}\([0-9]*\)\".*/\1/p")
ARCHIVE_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/warehouse/$LATEST_ID")
assert_status "$ARCHIVE_STATUS" "204" "archive warehouse by id"

log "8) Archived warehouse should be unavailable by ID"
GET_ARCHIVED_STATUS=$(curl -sS -o /dev/null -w "%{http_code}" "$BASE_URL/warehouse/$LATEST_ID")
assert_status "$GET_ARCHIVED_STATUS" "404" "get archived warehouse should return not found"

log "Warehouse UAT completed successfully."
