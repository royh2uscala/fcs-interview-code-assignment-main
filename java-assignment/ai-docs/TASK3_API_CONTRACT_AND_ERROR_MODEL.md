# Task 3 API Contract and Error Model

## Endpoint Contract (Current)

| Endpoint | Success Status | Notes |
| --- | --- | --- |
| `GET /warehouse` | `200` | Lists only active warehouses |
| `GET /warehouse/{id}` | `200` | Active warehouse only |
| `POST /warehouse` | `200` | Creates warehouse and returns created payload |
| `POST /warehouse/{businessUnitCode}/replacement` | `200` | Archives previous active warehouse and creates replacement |
| `DELETE /warehouse/{id}` | `204` | Archives warehouse (soft delete) |

## Error Status Convention

| Status | Meaning in Task 3 |
| --- | --- |
| `400` | Request/input/validation issue (invalid id format, invalid location, invalid capacity/stock payload) |
| `404` | Warehouse entity not found in active state |
| `409` | Business rule conflict (duplicate businessUnitCode, max warehouses reached for location) |

## Error Message Determinism

Warehouse handlers convert domain exceptions to deterministic status classes:
- `WarehouseValidationException` -> `400`
- `WarehouseNotFoundException` -> `404`
- `WarehouseConflictException` -> `409`

Persistence/constraint exceptions are translated to consistent API outcomes:
- Active business unit unique index violations -> `409`
- Capacity-stock check constraint violations -> `400`

## OpenAPI Alignment

`warehouse-openapi.yaml` was aligned so the create operation documents `200` to match runtime behavior.
