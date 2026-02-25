# Task 3 Traceability Matrix

## Requirement to Story Mapping

| Task 3 Requirement (`CODE_ASSIGNMENT.md`) | Story IDs (`TASK3_USER_STORIES...V02`) |
| --- | --- |
| Implement list/retrieve/create/replace/archive handlers and use cases | T3-US-01, T3-US-02, T3-US-03, T3-US-08, T3-US-12 |
| Business unit code verification | T3-US-04 |
| Location validation | T3-US-05, T3-US-11 |
| Warehouse creation feasibility (location max warehouses) | T3-US-06 |
| Capacity and stock validation | T3-US-07 |
| Replacement capacity accommodation | T3-US-10 |
| Replacement stock matching | T3-US-09 |

## Story to Endpoint/Use Case/Test Mapping

| Story | Endpoint(s) | Use Case / Component | Automated Test Coverage |
| --- | --- | --- | --- |
| T3-US-01 | `GET /warehouse` | `WarehouseRepository.getAll`, `WarehouseResourceImpl.listAllWarehousesUnits` | `WarehouseEndpointTest.testSimpleListWarehouses` |
| T3-US-02 | `GET /warehouse/{id}` | `WarehouseResourceImpl.getAWarehouseUnitByID` | `WarehouseEndpointTest.testGetWarehouseByIdShouldSupportHappyAndInvalidPaths` |
| T3-US-03 | `POST /warehouse` | `CreateWarehouseUseCase`, `WarehouseResourceImpl.createANewWarehouseUnit` | `WarehouseEndpointTest.testCreateWarehouseAndArchiveWarehouse` |
| T3-US-04 | `POST /warehouse` | `CreateWarehouseUseCase` | `WarehouseEndpointTest.testCreateWarehouseShouldFailWhenBusinessUnitCodeAlreadyExists`; `CreateWarehouseUseCaseTest.testCreateWarehouseShouldFailWhenBusinessUnitAlreadyExists` |
| T3-US-05 | `POST /warehouse`, replacement endpoint | `CreateWarehouseUseCase`, `ReplaceWarehouseUseCase` | `WarehouseEndpointTest.testCreateWarehouseShouldFailWhenLocationIsInvalid`; `ReplaceWarehouseUseCaseTest.testReplaceShouldFailWhenLocationIsInvalid` |
| T3-US-06 | `POST /warehouse` | `CreateWarehouseUseCase` | `WarehouseEndpointTest.testCreateWarehouseShouldFailWhenLocationMaxWarehousesReached`; `CreateWarehouseUseCaseTest.testCreateWarehouseShouldFailWhenMaxNumberReached` |
| T3-US-07 | `POST /warehouse` | `CreateWarehouseUseCase`, DB check constraint | `WarehouseEndpointTest.testCreateWarehouseShouldFailWhenCapacityLowerThanStock`; `WarehouseEndpointTest.testCreateWarehouseShouldFailWhenLocationCapacityExceeded`; `CreateWarehouseUseCaseTest.testCreateWarehouseShouldFailWhenStockGreaterThanCapacity` |
| T3-US-08 | `POST /warehouse/{businessUnitCode}/replacement` | `ReplaceWarehouseUseCase`, `WarehouseResourceImpl.replaceTheCurrentActiveWarehouse` | `WarehouseEndpointTest.testReplaceWarehouseSuccessAndValidationFailure` |
| T3-US-09 | replacement endpoint | `ReplaceWarehouseUseCase` | `WarehouseEndpointTest.testReplaceWarehouseSuccessAndValidationFailure`; `ReplaceWarehouseUseCaseTest.testReplaceShouldFailWhenStockDoesNotMatch` |
| T3-US-10 | replacement endpoint | `ReplaceWarehouseUseCase` | `ReplaceWarehouseUseCaseTest.testReplaceShouldFailWhenCapacityCannotAccommodateStock` |
| T3-US-11 | replacement endpoint | `ReplaceWarehouseUseCase` | `ReplaceWarehouseUseCaseTest.testReplaceShouldFailWhenTargetLocationMaxWarehousesReached`; `ReplaceWarehouseUseCaseTest.testReplaceShouldFailWhenLocationIsInvalid` |
| T3-US-12 | `DELETE /warehouse/{id}` | `ArchiveWarehouseUseCase`, `WarehouseResourceImpl.archiveAWarehouseUnitByID` | `WarehouseEndpointTest.testCreateWarehouseAndArchiveWarehouse`; `WarehouseEndpointTest.testArchiveWarehouseShouldValidateIdInputAndNotFound`; `ArchiveWarehouseUseCaseTest` |

## Concurrency and Runtime Risk Coverage

| Scenario | Mitigation | Test |
| --- | --- | --- |
| Concurrent duplicate create by same business unit | In-process lock manager + active unique DB index (`uk_warehouse_active_buc`) | `WarehouseEndpointTest.testConcurrentCreateWithSameBusinessUnitCodeShouldAllowOnlyOneActiveWarehouse` |
| Concurrent creates on location max constraint | In-process location lock in create/replace use cases | `WarehouseEndpointTest.testConcurrentCreateAtLocationLimitShouldNotExceedConfiguredMax` |
