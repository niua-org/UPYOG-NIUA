# Bugs and production issues observed during testing

No production code was changed in this pass; issues below are documented from test exploration and existing comments.

| Module | Class / area | Issue |
|--------|----------------|-------|
| billing-service | `ReceiptService` / `ReceiptServiceV2` | Redundant or ineffective null guard on bill request in receipt update path (see test class header comments). |
| finance-collections-voucher-consumer | `ReceiptService.updateReceipt` | Empty voucher list leads to `IndexOutOfBoundsException` instead of a controlled validation error (covered by test). |
| finance-collections-voucher-consumer | `EgfKafkaListener.process` | Malformed / empty JSON can yield empty `ReceiptReq` and NPE on backup logging path (invalid payload test documents behaviour). |
| collection-services | `EmployeeRepository` | Commented NPE risk on uninjected URL field (`hrEmployeePositonsUrl`). |
| collection-services | `ReceiptControllerV2` and related | Large blocks of receipt v2 logic commented out in source; tests removed by `remove-tests-for-uncompiled-sources.py`. |
| dashboard-analytics | `TarentoServiceImpl` / filters | Historical NPE on null filter maps when MDMS/chart config incomplete (guarded in new tests via fixtures). |
| egf-instrument | JSON mapping | `ObjectMapper.convertValue` type mismatches on instrument payloads (noted in earlier finance/billing integration tests). |

Add new rows here when tests expose regressions or contract bugs without fixing production in the same PR.
