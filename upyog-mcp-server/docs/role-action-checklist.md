# Role-action checklist

Status: **Roles received. Action rows still come from the access-control API, not from this file.**

Role catalog (tenant `pg`, module `ACCESSCONTROL-ROLES`) is stored in `docs/accesscontrol-roles.json`. That file lists role `code`, `name`, and `description`. It does not list action ids or action URLs.

Actions are read from the existing endpoint:

`POST /access/v1/actions/mdms/_get`

Request (`ActionRequest`):

- `RequestInfo` — built by the MCP server from the `auth-token` header
- `roleCodes` — codes on the user returned by `/user/_details`
- `tenantId` — validated tenant

Response: `actions[]` with `id`, `name`, `url`, `queryParams`, `serviceCode`, `enabled`.

`list_services` may hide an operation when none of the caller’s roles return a matching `url`. If the actions call fails, the server still lists the operation and lets the gateway decide. Do not hard-code action ids.

Candidate role codes from the supplied catalog (UX hint only):

| Service | Citizen | Employee roles named in the catalog |
|---|---|---|
| pgr | `CITIZEN` | `GO`, `RO`, `GA`, `GRO`, `DGRO`, `CSR`, `PGR-ADMIN`, `PGR_LME` |
| property | `CITIZEN` | `PT_CEMP`, `PT_DOC_VERIFIER`, `PT_FIELD_INSPECTOR`, `PT_APPROVER`, `PTADMIN`, `CEMP` |
| advertisement | `CITIZEN` | `ADS_CEMP` |
| venue-booking | `CITIZEN` | `CHB_CEMP`, `CHB_APPROVER`, `CHB_VERIFIER` |
| water tanker | `CITIZEN` | `WT_CEMP`, `WT_VENDOR`, `WT_DRIVER` |
| mobile toilet | `CITIZEN` | `MT_CEMP`, `MT_VENDOR`, `MT_DRIVER` |
| tree pruning | `CITIZEN` | `TP_CEMP`, `TP_VERIFIER`, `TP_EXECUTION`, `TP_VENDOR` |

`SUPERUSER` is described as access to all screens. That description is not a substitute for an action row.

Authorization that is verified:

- Gateway `RbacFilter` calls `POST /access/v1/actions/_authorize`
- The `uri` sent to access-control is the incoming gateway path
- The UI loads actions with `POST /access/v1/actions/mdms/_get`

Role code, action id, and the stored action URL are **UNKNOWN** until the target tenant’s MDMS `ACCESSCONTROL` data is provided. Do not invent `CITIZEN` action ids. The legacy SQL files under `finance/egov/**/roleaction*.sql` are not this checklist.

MCP role filtering is a display hint only. Gateway plus egov-accesscontrol stay authoritative.

Fill the empty columns from MDMS before `allowedRolesHint` is treated as accurate.

| Service | Operation | Gateway path the RBAC `uri` must match | Role code | Action id | Action URL in MDMS | Module | Status |
|---|---|---|---|---|---|---|---|
| pgr | create | `/pgr-services/v2/request/_create` | UNKNOWN | UNKNOWN | UNKNOWN | PGR | needs MDMS |
| pgr | search | `/pgr-services/v2/request/_search` | UNKNOWN | UNKNOWN | UNKNOWN | PGR | needs MDMS |
| pgr | count | `/pgr-services/v2/request/_count` | UNKNOWN | UNKNOWN | UNKNOWN | PGR | not an MCP tool in v1 |
| property | search | `/property-services/property/_search` | UNKNOWN | UNKNOWN | UNKNOWN | PT | needs MDMS |
| advertisement | create | `/adv-services/booking/v1/_create` | UNKNOWN | UNKNOWN | UNKNOWN | Advertisement | needs MDMS |
| advertisement | search | `/adv-services/booking/v1/_search` | UNKNOWN | UNKNOWN | UNKNOWN | Advertisement | needs MDMS |
| advertisement | slot search | `/adv-services/booking/v1/_slot-search` | UNKNOWN | UNKNOWN | UNKNOWN | Advertisement | needed before create if the UI always calls it |
| venue | create | `/chb-services/booking/v1/_create` | UNKNOWN | UNKNOWN | UNKNOWN | CHB | needs MDMS |
| venue | search | `/chb-services/booking/v1/_search` | UNKNOWN | UNKNOWN | UNKNOWN | CHB | needs MDMS |
| venue | cancel update | `/chb-services/booking/v1/_update` | UNKNOWN | UNKNOWN | UNKNOWN | CHB | needs MDMS |
| venue | refund workflow | `/collection-services/payments/CHB/_workflow` | UNKNOWN | UNKNOWN | UNKNOWN | collection | contract not fully read |
| billing | fetch bill | `/billing-service/bill/v2/_fetchbill` | UNKNOWN | UNKNOWN | UNKNOWN | BillingService | needs MDMS |
| mdms | search | `/egov-mdms-service/v1/_search` | UNKNOWN | UNKNOWN | UNKNOWN | MDMS | gateway route exists |
| user | token details | `/user/_details` | n/a | n/a | n/a | egov-user | gateway calls this; not an MCP tool |
| access | authorize | `/access/v1/actions/_authorize` | n/a | n/a | n/a | egov-accesscontrol | gateway calls this; not an MCP tool |
| water tanker | create/search/update | `/request-service/water-tanker/v1/_create`, `_search`, `_update` | UNKNOWN | UNKNOWN | UNKNOWN | request-service | later descriptor |
| mobile toilet | create/search/update | `/request-service/mobile-toilet/v1/_create`, `_search`, `_update` | UNKNOWN | UNKNOWN | UNKNOWN | request-service | later descriptor |
| tree pruning | create/search/update | `/tp-services/tree-pruning/v1/_create`, `_search`, `_update` | UNKNOWN | UNKNOWN | UNKNOWN | tp-services | create blocked on documents in MCP v1 |

Employee workflow actions (assign, approve, reject, resolve) are out of v1 even after rows exist.
