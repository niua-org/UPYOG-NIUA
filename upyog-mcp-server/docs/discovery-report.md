# UPYOG MCP Server — Discovery Report

Status: **Review decisions recorded. Gateway routes added. Server implementation not started.**

Date: 2026-10-06

## Review decisions (2026-10-06)

| Topic | Decision |
|---|---|
| Runtime | Java 25. Spring Boot **3.5.5 or newer** (3.5.x current docs: 3.5.16). Spring AI **1.1.x** `spring-ai-starter-mcp-server-webmvc` with `spring.ai.mcp.server.protocol=STREAMABLE`. |
| Why not Boot 3.4 | Boot 3.4.13 supports Java 17 through 24 only. Java 25 support starts at Boot 3.5.5. Spring AI 1.1.x targets Boot 3.5.x. Downstream UPYOG services and `org.egov.tracer` stay on Boot 3.2.2. |
| Session header | `auth-token` |
| Masters | `egov-mdms-service` `POST /egov-mdms-service/v1/_search`. Not mdms-v2. |
| Bills | Return the billing JSON. The list field is `Bill`. `ResposneInfo` is the info field spelled that way in source. `hasPendingBill` is derived only from that `Bill` array (non-empty means a bill was returned). Do not invent a second bill schema. `_fetchbill` may generate a bill when none is active, so an empty list is whatever the API actually returns after that attempt. |
| Roles | Catalog supplied for tenant `pg`, module `ACCESSCONTROL-ROLES`, saved at `docs/accesscontrol-roles.json`. These are role definitions, not action grants. |
| Actions | Loaded at runtime from `POST /access/v1/actions/mdms/_get`. Body is `ActionRequest`: server-built `RequestInfo`, `roleCodes` from the authenticated user, `tenantId`. Response `actions[]` has `id`, `name`, `url`, `serviceCode`. Gateway RBAC stays authoritative. Role hints are a UX filter over that response. |
| Gateway routes | Added in `core-services/gateway/src/main/resources/routes.properties` for property, billing, adv, chb, request, tree pruning, and collection. No `StripPrefix`, same pattern as PGR, because each service context path is the public prefix. Host names match `build/build-config.yml` image names. Duplicate route index 18 (`edcr` and `egov-pdf`) is split so `egov-pdf` is index 19. |
| Venue cancel | Deferred. Do not implement the collection refund plus booking update until a later phase. |
| Documents | Use filestore. `POST /filestore/v1/files` (`multipart` field `file`, query `tenantId`, `module`, optional `tag`, optional `requestInfo`). Response is `StorageResponse.files[]` with `fileStoreId` and `tenantId`. The MCP server uploads through the gateway, then places that id on the descriptor document field. The agent does not supply a file-store id or a URL. |

Repository inspected:

`/Users/keshav/Documents/workspace-niua/niua-keshavmca12/Keshav-personal-UPYOG-NIUA`

Branch observed: `niua-dev-3.0`

The `upyog-mcp-server/` directory was empty at the start of discovery. This report is the first artifact in that directory.

Labels used below:

- **VERIFIED** — read from source, configuration, or UI code in this repository
- **ASSUMPTION** — a working interpretation that still needs confirmation
- **UNKNOWN** — not present in this repository; not guessed
- **BLOCKER** — must be resolved before the affected implementation step

---

## 1. Verified architecture

**VERIFIED.** UPYOG is a multi-module monorepo. The MCP server must be a separate stateless process. It must not copy billing, workflow, or validation logic out of the existing services.

Intended call path, matching the existing citizen UI:

```
AI agent
  -> UPYOG MCP server (this new service)
  -> UPYOG API Gateway
  -> existing microservice
```

Evidence that the UI already talks to the gateway by context path, and that each service mounts that same context path:

| Service | Servlet context path | Evidence |
|---|---|---|
| pgr-services | `/pgr-services` | `municipal-services/pgr-services/src/main/resources/application.properties` |
| property-services | `/property-services` | `municipal-services/property-services/src/main/resources/application.properties` |
| billing-service | `/billing-service` | `business-services/billing-service/src/main/resources/application.properties` |
| advertisement-service | `/adv-services` | `municipal-services/advertisement-service/src/main/resources/application.properties` |
| community-hall-booking | `/chb-services` | `municipal-services/community-hall-booking/src/main/resources/application.properties` |
| request-service | `/request-service` | `municipal-services/request-service/src/main/resources/application.properties` |
| tp-services (tree pruning) | `/tp-services` | `municipal-services/tp-services/src/main/resources/application.properties` |
| egov-user | `/user` | `core-services/egov-user/src/main/resources/application.properties` |
| egov-accesscontrol | `/access` | `core-services/egov-accesscontrol/src/main/resources/application.properties` |
| egov-mdms-service | `/egov-mdms-service` | `core-services/egov-mdms-service/src/main/resources/application.properties` |
| egov-workflow-v2 | `/egov-workflow-v2` | `core-services/egov-workflow-v2/src/main/resources/application.properties` |

Citizen UI paths are centralized in:

`frontend/upyog-ui/web/micro-ui-internals/packages/libraries/src/services/atoms/urls.js`

**BLOCKER — checked-in gateway route table is incomplete.**

`core-services/gateway/src/main/resources/routes.properties` only defines routes 0–18. It includes `pgr-services` (`Path=/pgr-services/**`) and core services such as `user`, `access`, `egov-mdms-service`, and `egov-workflow-v2`. It does **not** define routes for:

- `/property-services/**`
- `/billing-service/**`
- `/adv-services/**`
- `/chb-services/**`
- `/request-service/**`
- `/tp-services/**`
- `/collection-services/**`

Route index 18 is also declared twice (`edcr` and `egov-pdf`). No other `Path=/property-services` (or equivalent) route definition was found in `*.properties`, `*.yml`, or `*.yaml` in this repository.

The UI and the service context paths agree on the public paths. The deployed gateway ConfigMap that actually forwards those paths is **not in this checkout**.

What this means: the MCP server must call the UI gateway paths, not pod DNS names. Before production, the environment gateway route table must be confirmed. Do not add direct service URLs as a workaround.

**VERIFIED.** Almost every citizen API in scope is `POST`. Search criteria are query parameters (`@ModelAttribute`). `RequestInfo` is a JSON body field named `RequestInfo` (Pascal case). The agent must never supply that object.

---

## 2. Service / operation matrix

Gateway path = servlet context path + controller mapping. HTTP method is POST for every row. Search and fetch operations put filters in the query string and `RequestInfo` in the body.

Pagination columns mean what the service actually accepts. None of these search APIs echo a page object. Callers send `limit` and `offset` (property uses `Long`; others use `Integer`).

### 2.1 PGR — `pgr-services`

Controller: `municipal-services/pgr-services/src/main/java/org/egov/pgr/web/controllers/RequestsApiController.java`

| Item | create | search | status |
|---|---|---|---|
| Gateway path | `/pgr-services/v2/request/_create` | `/pgr-services/v2/request/_search` | Same as search, filtered by `serviceRequestId` |
| HTTP | POST | POST | POST |
| Request model | `ServiceRequest` | Body: `RequestInfoWrapper`. Query: `RequestSearchCriteria` | Same as search |
| Response model | `ServiceResponse` | `ServiceResponse` | `ServiceResponse` |
| Identifier | `service.serviceRequestId` (also `service.id`) | query `serviceRequestId` or `ids` | `serviceRequestId` |
| Status field | `service.applicationStatus` | same | same |
| Workflow businessService | `PGR` (`PGRConstants.PGR_BUSINESSSERVICE`) | n/a | `PGR` |
| Workflow action on citizen create | Timeline code looks up `performedAction === "APPLY"`. Constant `PGRConstants.APPLY = "APPLY"`. The create form assignment itself was not isolated to one submit function. | n/a | Read-only |
| Billing | none | none | none |

**Required on create** (`Service.java` `@NotNull`):

- `RequestInfo` — server-generated, never from the agent
- `service.tenantId`
- `service.serviceCode`
- `service.source`
- `service.address`
- `service.priority` enum: `LOW`, `MEDIUM`, `HIGH`

**Optional on create:** `description`, `accountId`, `rating` (1–5), `additionalDetail` (max 600 chars via `@CharacterConstraint`), `workflow` (`action`, `assignes`, `comments`, `verificationDocuments`).

Address fields (`Address.java`): `tenantId`, `doorNo`, `plotNo`, `id`, `landmark`, `city`, `district`, `region`, `state`, `country`, `pincode`, `additionDetails`, `buildingName`, `street`, `locality` (`Boundary`), `geoLocation`. The class comment says any one of address, lat/long, or address id is mandatory. Bean validation only marks the `address` object `@NotNull`.

**Search query fields** (`RequestSearchCriteria.java`): `tenantId`, `tenantIds`, `serviceCode`, `applicationStatus`, `mobileNumber`, `serviceRequestId`, `sortBy` (`locality`, `applicationStatus`, `serviceRequestId`), `sortOrder` (`ASC`, `DESC`), `locality`, `ids`, `fromDate`, `toDate`, `slaDeltaMaxLimit`, `slaDeltaMinLimit`, `limit`, `offset`, `accountId`. `userIds` and `isPlainSearch` are `@JsonIgnore`.

**Pagination:** `pgr.default.limit=100`, `pgr.search.max.limit=200` in pgr `application.properties`. Response list is `ServiceWrappers`. Extra aggregates: `complaintsResolved`, `averageResolutionTime`, `complaintTypes`. No response page object.

**MDMS:** module `RAINMAKER-PGR`, master `ServiceDefs`. Filter used by the service: `$.MdmsRes.RAINMAKER-PGR.ServiceDefs[?(@.serviceCode=='{SERVICEDEF}')]`. SLA field name in that master: `slaHours`.

**Documents:** workflow verification documents exist. File upload is out of scope for MCP v1. No verified PGR create deep link was isolated beyond the API. Do not invent a UI URL.

**Other PGR endpoints that exist and are out of v1 tool scope:** `/v2/request/_update`, `/v2/request/_count`, `/v2/request/_plainsearch`. Employee actions (`ASSIGN`, `RESOLVE`, `REJECT`, `REASSIGN`, `REOPEN`, `RATE`, and the `*_CITIZEN_*` / `*_EMPLOYEE_*` composites in `PGRConstants`) are explicitly non-goals.

### 2.2 Property — `property-services`

Controller: `municipal-services/property-services/src/main/java/org/egov/pt/web/controllers/PropertyController.java`

| Item | search |
|---|---|
| Gateway path | `/property-services/property/_search` |
| HTTP | POST |
| Request | Body: `RequestInfoWrapper`. Query: `PropertyCriteria` |
| Response | `PropertyResponse` with `properties` and `count` |
| Identifier | `propertyId` |
| Status | `Status` enum: `ACTIVE`, `INACTIVE` |
| Workflow businessService | Search does not transition workflow. Create/update names in config: `PT.CREATE`, `PT.LEGACY`, `PT.MUTATION` |
| Billing businessService | `PT` (also `PT.MUTATION` in `pt.business.codes`) |
| MDMS module | `PropertyTax` |

**Search query fields** (`PropertyCriteria.java`): `tenantId`, `propertyIds`, `tenantIds`, `acknowledgementIds`, `uuids`, `oldpropertyids`, `status`, `mobileNumber`, `name`, `ownerIds`, `audit`, `offset`, `limit`, `fromDate`, `toDate`, `locality`, `doorNo`, `oldPropertyId`, `propertyType`, `creationReason`, `documentNumbers`, plus internal flags (`isSearchInternal`, `isInboxSearch`, `isDefaulterNoticeSearch`, `isRequestForDuplicatePropertyValidation`, `isCitizen`, `isRequestForCount`, `isRequestForOldDataEncryption`).

**Validation** (`PropertyValidator.validatePropertyCriteria`):

- On a central instance, `tenantId` is mandatory and must have the configured number of levels.
- Open search requires `propertyIds` or `mobileNumber`.
- `fromDate` and `toDate` must be supplied together.
- Citizens: if no other criterion is present, the service sets `mobileNumber` from the authenticated user. Allowed param tokens: `accountId,ids,propertyDetailids,mobileNumber,oldpropertyids,ownerids,name` (`citizen.allowed.search.params`).
- Employees: tenant-only search is rejected. Allowed tokens: `accountId,ids,propertyDetailids,mobileNumber,oldpropertyids,name`.
- The allow-list token `ids` is checked against the `propertyIds` collection. The query parameter name is `propertyIds`.

**Response pagination:** `count` is set only when `isRequestForCount=true`; otherwise `properties` is populated and `count` stays 0. Do not invent a page wrapper.

**v1 scope:** search only, plus pending bill and payment link through the generic billing tools. Do not expose `_create` or `_update`.

### 2.3 Advertisement — `advertisement-service`

Controller: `municipal-services/advertisement-service/src/main/java/org/upyog/adv/web/controllers/AdvertisementServiceApiController.java`

| Item | create | search |
|---|---|---|
| Gateway path | `/adv-services/booking/v1/_create` | `/adv-services/booking/v1/_search` |
| HTTP | POST | POST |
| Request | `BookingRequest` | Body: `RequestInfoWrapper`. Query: `AdvertisementSearchCriteria` |
| Response | `AdvertisementResponse.bookingApplication` | same, plus `count` |
| Identifier | `bookingNo`, `bookingId` | query `bookingNo`, `bookingIds` |
| Status | `bookingStatus` | query `status` |
| Billing businessService | `adv-services` (`adv.business.service.name`) | same |
| Module name | `Advertisement` (`adv.module.name`) | same |

**Create body** (`BookingRequest`): `RequestInfo` (server-side), `bookingApplication` (`BookingDetail`), `isDraftApplication`.

`BookingDetail` fields: `bookingId`, `bookingNo`, `paymentDate`, `draftId`, `applicationDate`, `tenantId`, `bookingStatus`, `receiptNo`, `permissionLetterFilestoreId`, `paymentReceiptFilestoreId`, `cartDetails` (`@NotNull` in `CreateApplicationGroup`), `documents` / `uploadedDocumentDetails`, `applicantDetail`, `address`, `auditDetails`.

**Documents:** create carries document file-store ids. MCP v1 must not upload files. If a create cannot proceed without documents, return the v1 document message. A verified advertisement-create UI deep link was not isolated. Do not invent one.

**Search query fields:** `tenantId`, `bookingIds`, `status`, `applicantName`, `faceArea`, `bookingNo`, `mobileNumber`, `offset`, `limit`, `fromDate`, `toDate` (`@ValidDate`), `isDraftApplication`. `createdBy` is `@JsonIgnore`.

**Related endpoints not in the initial tool set:** `/adv-services/booking/v1/_slot-search`, `/_estimate`, `/_update`, `/_deletedraft`. Slot search and estimate are likely required before a safe create. **UNKNOWN** whether create can succeed without a prior slot search and estimate. Treat that as a descriptor/adapter question before write implementation.

**MDMS used by the service** (`MdmsUtil`, `BookingConstants`): module from `adv.module.name` = `Advertisement`; masters `CalculationType`, `TaxAmount`; billing tax heads filtered by `$.[?(@.service=='adv-services')]`; common module `common-masters`. Voice-bot config also lists `AdType`, `Location`, `FaceArea`, `NightLight`. Those names are in `upyog-voice-bot/config.yml`, not in the Java MDMS validator. Mark `AdType` / `Location` / `FaceArea` as **verified only as the voice-bot client contract**, and confirm against MDMS data before allow-listing.

**Workflow action names for create:** not a constant in `BookingConstants`. Do not invent `APPLY`.

### 2.4 Community hall / venue booking — `community-hall-booking`

Controller: `municipal-services/community-hall-booking/src/main/java/org/upyog/chb/web/controllers/CommunityHallBookingController.java`

| Item | create | search | cancel |
|---|---|---|---|
| Gateway path | `/chb-services/booking/v1/_create` | `/chb-services/booking/v1/_search` | `/chb-services/booking/v1/_update` |
| HTTP | POST | POST | POST |
| Request | `VenueBookingRequest` | Body: `RequestInfoWrapper`. Query: `VenueBookingSearchCriteria` | `VenueBookingRequest` with `bookingStatus=CANCELLED` |
| Response | `CommunityHallBookingResponse.venueBookingApplication` | same, plus `count` | updated booking |
| Identifier | `bookingNo`, `bookingId` | `bookingNo`, `bookingIds` | existing `bookingNo` |
| Status | `bookingStatus` | query `status` | UI sets `CANCELLED` |
| Billing businessService | `chb-services` (`chb.business.service.name`) | same | same for bills |
| Workflow businessService for refunds | `booking-refund`, module `chb-services` | n/a | see cancel note |

**Create required fields** (`VenueBookingDetail`): `specialCategory`, `purpose`, `bookingSlotDetails`. Other fields: `tenantId`, `venueCode`, `venueName`, `venueType`, `purposeDescription`, `applicantDetail`, `address`, `documents`, `workflow` (`ProcessInstance`).

**Search query fields:** `tenantId`, `bookingIds`, `status`, `venueCode`, `venueType`, `bookingNo`, `mobileNumber`, `offset`, `limit`, `fromDate`, `toDate`. `createdBy` is `@JsonIgnore`.

**Cancel is not a single status write. VERIFIED in the citizen UI** (`frontend/.../chb/src/components/SearchApplication.js`):

1. Load the booking.
2. If the receipt `paymentMode` is `ONLINE`, call collection workflow:
   - `POST /collection-services/payments/CHB/_workflow?tenantId=`
   - Body `PaymentWorkflows[]` with `action: "REFUND"`
   - The business-service segment here is `CHB`, which is **not** the same string as billing `chb-services`.
3. Then `POST /chb-services/booking/v1/_update` with `bookingStatus: "CANCELLED"` and `additionalDetails.cancellationReason`.

Refund success and booking cancel are independent in the UI. The UI surfaces “cancelled, but refund initiation failed”. MCP must not calculate a refund amount. Backend terms must be returned from collection/billing, not computed locally.

**UNKNOWN:** the collection `_workflow` request schema beyond the UI payload fields `paymentId`, `action`, `tenantId`, `reason`. The collection controller was not fully traced in this pass. Cancel must not be implemented until that contract is read.

**MDMS module:** `CHB` (`$.MdmsRes.CHB`). Masters: `Purpose`, `SpecialCategory`, `CalculationType`, `CommunityHalls`, `HallCode`, `Parks`, `ParkCode`, `GuestHouses`, `GuestHouseCode`, `Stadiums`, `StadiumCode`, `Crematoriums`, `CrematoriumCode`, `VenueType`, `Documents`. Common module: `common-masters`.

**Related endpoints not in the initial three operations:** `/v1/_init`, `/v1/_slot-search`, `/v1/_estimate`, `/booking/trigger-workflow-update`. Slot search is how the UI finds bookable slots. **UNKNOWN** whether `_create` is valid without a prior slot search.

### 2.5 Request services — water tanker and mobile toilet

Controller: `municipal-services/request-service/src/main/java/org/upyog/rs/web/controllers/RequestServiceController.java`

Tree pruning is a **different service**: `municipal-services/tp-services`, context `/tp-services`.

| Operation | Gateway path | Body | Query | Response list | Id | Status | Billing / workflow module |
|---|---|---|---|---|---|---|---|
| Water tanker create | `/request-service/water-tanker/v1/_create` | `WaterTankerBookingRequest` (`waterTankerBookingDetail`) | none | `waterTankerBookingApplication` | `bookingNo` | `bookingStatus` | business service `watertanker`; module `request-service.water_tanker` |
| Water tanker search | `/request-service/water-tanker/v1/_search` | `RequestInfoWrapper` | `WaterTankerBookingSearchCriteria` | `waterTankerBookingDetails` + `count` | `bookingNo` | `status` query | same |
| Water tanker update | `/request-service/water-tanker/v1/_update` | `WaterTankerBookingRequest` | none | same as create | same | same | workflow actions in code: `APPROVE`, `PAY`, `RATE`, `REJECT` |
| Mobile toilet create | `/request-service/mobile-toilet/v1/_create` | `MobileToiletBookingRequest` | none | `mobileToiletBookingApplication` | `bookingNo` | `bookingStatus` | business service `mobileToilet`; module `request-service.mobile_toilet` |
| Mobile toilet search | `/request-service/mobile-toilet/v1/_search` | `RequestInfoWrapper` | `MobileToiletBookingSearchCriteria` | `mobileToiletBookingDetails` + `count` | `bookingNo` | query `status` | same |
| Mobile toilet update | `/request-service/mobile-toilet/v1/_update` | `MobileToiletBookingRequest` | none | same as create | same | same | same |
| Tree pruning create | `/tp-services/tree-pruning/v1/_create` | `TreePruningBookingRequest` | none | `treePruningBookingApplication` | `bookingNo` | `bookingStatus` | business service `treePruning`; module `request-service.tree_pruning` |
| Tree pruning search | `/tp-services/tree-pruning/v1/_search` | `RequestInfoWrapper` | `TreePruningBookingSearchCriteria` | `treePruningBookingDetails` + `count` | `bookingNo` | query `status` | same |
| Tree pruning update | `/tp-services/tree-pruning/v1/_update` | `TreePruningBookingRequest` | none | same as create | same | same | `ACTION_PAY` exists |

**Water tanker required:** `tankerQuantity`, `waterQuantity`, `deliveryDate`, `deliveryTime`, `applicantDetail`, `address`, `workflow`. Other fields include `tankerType`, `waterType`, `description`, `tenantId`, `localityCode`, `mobileNumber`.

**Mobile toilet required:** `noOfMobileToilet`, `deliveryFromDate`, `deliveryToDate`, `deliveryFromTime`, `deliveryToTime`, `applicantDetail`, `address`, `workflow`.

**Tree pruning required:** `applicantDetail`, `address`, `workflow`, `documentDetails` (`@NotNull`). Create **requires documents**. MCP v1 cannot upload files, so tree-pruning create cannot be completed through MCP until a non-document path is verified. None was found. Return the v1 document message. Do not invent a UI deep link.

**Search query fields** (water tanker; mobile toilet and tree pruning follow the same shape): `tenantId`, `status`, `bookingNo`, `mobileNumber`, `offset`, `limit`, `localityCode`, `fromDate`, `toDate`. `createdBy` is ignored on input.

**MDMS module:** `Request-Service`. Masters: `WaterTankerCalculationType`, `TankerDeliveryTimeCalculationType`, `MobileToiletCalculationType`. Tree pruning master: `TreePruningCalculationType`.

**Workflow action on citizen create:** `Workflow.action` is required by the model, but the citizen submit value was not found as a Java constant. Do not hard-code `APPLY` for these modules.

These three services are documented because the brief asked for them. The phase-1 descriptor list in the brief is only PGR, property, advertisement, and venue booking. Water tanker, mobile toilet, and tree pruning should be later descriptors, not new tools.

### 2.6 Billing — pending bill

Controller: `business-services/billing-service/src/main/java/org/egov/demand/web/controller/BillControllerv2.java`

| Item | Value |
|---|---|
| Gateway path | `/billing-service/bill/v2/_fetchbill` |
| HTTP | POST |
| Body | `RequestInfoWrapper` |
| Query | `GenerateBillCriteria` |
| Response | `BillResponseV2` |
| Response bill list | JSON field `Bill` |
| Response info field | JSON field `ResposneInfo` (spelled that way in source) |

**Required query fields:** `tenantId` (`@NotNull`, max 256), `businessService` (`@NotNull`, max 256).

**Optional:** `demandId` (max 64), `consumerCode` (a `Set<String>`), `email`, `mobileNumber` (pattern `^[0-9]{10}$`).

`toDemandCriteria()` copies `consumerCode` and sets `isPaymentCompleted=false`. `toBillSearchCriteria()` searches `BillStatus.ACTIVE`.

**UNKNOWN:** the exact JSON when nothing is due (empty `Bill` list versus an error). The MCP tool must map the real response to `{ "hasPendingBill": false }` only after a contract test shows the empty case. Do not invent a local “no bill” calculation.

`_generate` is deprecated and throws `EG_BS_API_ERROR`. Do not call it.

`_create` and `_cancelbill` are out of scope. Payment initiation is out of scope.

### 2.7 Payment link

**VERIFIED citizen route** in the current UI:

- Parent: `frontend/.../common/src/payments/citizen/index.js` — `my-bills/:businessService/*`
- Child: `frontend/.../common/src/payments/citizen/bills/index.js` — `:consumerCode`

Concrete links used by the UI:

| Module | Path |
|---|---|
| Property tax | `/upyog-ui/citizen/payment/my-bills/PT/{propertyId}` |
| Advertisement | `/upyog-ui/citizen/payment/my-bills/adv-services/{bookingNo}` |
| Community hall | `/upyog-ui/citizen/payment/my-bills/chb-services/{bookingNo}` |

`MyBills` reads `tenantId` from the logged-in user, router state, or the `tenantId` query parameter. If none is present it redirects to `/upyog-ui/citizen/login`. A link opened by an already logged-in citizen can work without `tenantId` in the query. A cold link should include `?tenantId=` because the page reads it.

**Do not use** the older notification templates that still say `digit-ui/citizen/payment/my-bills/...`. Those remain in property, request-service, tp-services, and community-hall `application.properties`. The React app in this repo uses the `upyog-ui` prefix.

The payment page fetches bills. It does not mean the MCP server created a payment. `get_payment_link` must only return this URL after `_fetchbill` shows a payable bill.

**UNKNOWN:** the public origin to prefix (`https://niuatt.niua.in` appears only in `upyog-voice-bot/config.yml` as one environment). The MCP server must take the UI base URL from configuration, not hard-code an environment.

### 2.8 MDMS search

| Item | Value |
|---|---|
| Gateway path | `/egov-mdms-service/v1/_search` |
| HTTP | POST |
| Body | `MdmsCriteriaReq` (`RequestInfo` + criteria) |
| Gateway route | **VERIFIED** in `routes.properties` (`Path=/egov-mdms-service/**`) |

There is also `mdms-v2`. Community hall config has `upyog.mdms.v2.enabled=false` and endpoint `mdms-v2/v1/_search`. Voice-bot PGR config points at `/mdms-v2/v1/_search` while the Java PGR constant still names module `RAINMAKER-PGR`. **UNKNOWN** which deployment is authoritative for complaint types. Do not pick mdms-v2 until the target environment says v2 is on.

### 2.9 Workflow read (for status timelines)

| Item | Value |
|---|---|
| Process search | `POST /egov-workflow-v2/egov-wf/process/_search` |
| Business service search | `POST /egov-workflow-v2/egov-wf/businessservice/_search` |
| Gateway route | **VERIFIED** (`Path=/egov-workflow-v2/**`) |

Services call these internally. The MCP status tool should prefer the module search response (which already includes status) and call workflow only when the module response does not contain the timeline. Process-search query parameter names were not fully extracted. **UNKNOWN** until `ProcessInstanceSearchCriteria` is read during implementation. Do not invent `history` fields.

---

## 3. Authentication flow

**VERIFIED.** Gateway order (`GlobalFilter.getOrder`, lower runs first):

1. `RequestStartTimeFilter` (0)
2. `CorrelationIdFilter` (1)
3. `AuthPreCheckFilter` (2)
4. `RbacPreCheckFilter` (3) and `PreHookFilter` (3)
5. `AuthFilter` (4)
6. `RbacFilter` (5)
7. `RequestEnrichmentFilter` (6)

### 3.1 Incoming auth token

`AuthPreCheckFilterHelper` reads `RequestInfo.authToken` from the JSON field `RequestInfo`. If the body is absent it reads the `Auth-Token` header. Open endpoints skip auth. Mixed-mode endpoints allow a missing token. Everything else without a token returns 401 `"You are not authorized to access this resource"`.

Local gateway defaults (`application.properties`):

- `egov.auth-service-host=http://localhost:8081/`
- `egov.auth-service-uri=user/_details?access_token=`
- `egov.user.search.path=/user/v1/_search`

### 3.2 egov-user token validation

`UserUtils.getUser` POSTs to `{authServiceHost}{authUri}{authToken}`, which is:

`POST /user/_details?access_token={token}`

with a null body. `UserController` maps `POST /_details`. The returned `User` is written back onto `RequestInfo.userInfo` by `AuthCheckFilterHelper`. The client-supplied `userInfo` is replaced.

User fields (`services-common` `User.java`): `id`, `userName`, `name`, `type`, `mobileNumber`, `emailId`, `roles`, `tenantId`, `uuid`.

Roles are on the user object (`Role.code`, `Role.name`, `Role.tenantId`), logged by the gateway before RBAC.

### 3.3 What the MCP server must do

**VERIFIED reuse path.** The MCP server should:

1. Accept the citizen or employee token from the MCP client transport (header), never as a tool argument.
2. Call gateway `POST /user/_details?access_token=...` **or** send the token inside server-built `RequestInfo` and let the gateway call `/_details`.
3. Use the returned `uuid`, `type`, `roles`, and `tenantId` as the only identity.
4. Forward that same token inside `RequestInfo.authToken` on every downstream call.

**Do not** create a second RBAC model, a shared superuser token, or a service account that impersonates citizens. The voice-bot `master_user` block in `upyog-voice-bot/config.yml` is a fallback system profile. The MCP server must not copy that pattern.

**UNKNOWN:** the exact MCP client header the UPYOG Assistant will use to pass the existing session token (`Authorization: Bearer` versus `auth-token`). That is an integration contract with the chatbot team. See `docs/chatbot-team-guide.md`.

Login and OTP are non-goals. Token issuance stays in `POST /user/oauth/token` (`upyog-voice-bot` config and egov-user). The MCP server does not log users in.

---

## 4. Authorization flow

**VERIFIED.** `RbacFilterHelper`:

1. Requires `RequestInfo.userInfo`. If auth was skipped, RBAC is skipped.
2. Resolves tenant ids from the request (`CommonUtils.validateRequestAndSetRequestTenantId`).
3. POSTs to `egov.authorize.access.control.host + egov.authorize.access.control.uri`.
4. Local default: `http://localhost:8092/access/v1/actions/_authorize`.
5. Body is `AuthorizationRequestWrapper` of the incoming `RequestInfo` plus `roles`, `uri` (the incoming path), and `tenantIds`.
6. HTTP 200 means allowed. Any other result is 401.

Access-control remains authoritative. MCP role filtering is only a hint for `list_services`.

`RequestEnrichmentFilter` then:

- sets `RequestInfo.correlationId` from the gateway correlation id
- adds header `x-correlation-id`
- adds `x-pass-through-gateway: true`
- adds `x-user-info` when the body is not JSON-compatible

The downstream service receives the gateway-enriched body, not the raw client body.

---

## 5. RequestInfo flow

**VERIFIED** model: `core-services/libraries/services-common/src/main/java/org/egov/common/contract/request/RequestInfo.java`

| Field | Role |
|---|---|
| `apiId` | API identifier string |
| `ver` | version string |
| `ts` | epoch millis (`Long`) |
| `action` | optional action string on RequestInfo itself (distinct from workflow `action`) |
| `did` | device id |
| `key` | present on the model; must not carry secrets into logs |
| `msgId` | message id |
| `authToken` | the user token |
| `correlationId` | gateway overwrites this |
| `userInfo` | gateway overwrites this from `/user/_details` |

JSON name is `RequestInfo`, not `requestInfo`. Tracer accepts both, but gateway auth reads the Pascal-case field (`REQUEST_INFO_FIELD_NAME_PASCAL_CASE`).

**MCP rule:** the server builds `RequestInfo`. Tool arguments must not include `RequestInfo`, `authToken`, `userInfo`, `uuid`, or `roles`. A business `tenantId` is allowed only after it is checked against the authenticated user's tenant and roles. The exact comparison rule is **UNKNOWN**: gateway tenant logic is in `CommonUtils.validateRequestAndSetRequestTenantId`, which was not fully expanded in this pass. Implementation must read that method before coding `TenantValidator`. Do not invent “user.tenantId equals argument” as the only rule; employees hold multiple role tenants.

---

## 6. Tracing flow

**VERIFIED.** In-repo tracer is already on the target stack.

| Item | Value |
|---|---|
| Artifact | `org.egov.services:tracer:2.9.0-SNAPSHOT` |
| Parent | Spring Boot `3.2.2` |
| Java | 17 |
| Servlet | `jakarta.servlet` (`TracerFilter`) |
| Source | `core-services/libraries/tracer` |

Correlation behavior (`TracerFilter`, `TracerConstants`):

1. Header `x-correlation-id`
2. Else `RequestInfo.correlationId` or `requestInfo.correlationId` in a JSON POST body
3. Else a generated UUID
4. MDC key `CORRELATION_ID`
5. Header `tenantId` copied to MDC `TENANTID`

Gateway uses the same header name `x-correlation-id` and the same MDC key `CORRELATION_ID`, then writes it onto `RequestInfo.correlationId`.

**Do not upgrade tracer.** It is already Boot 3.2.2. The MCP server can either depend on this artifact or reproduce the same header, MDC key, and `RequestInfo.correlationId` field. Reproducing the three keys is the smaller option if the MCP SDK forces a different Spring generation (see compatibility). Semantics to preserve: header `x-correlation-id`, MDC `CORRELATION_ID`, body field `RequestInfo.correlationId`.

Tracer logs the raw request body when request logging is enabled (`REQUEST_BODY_LOG_MESSAGE`). That is unsafe for the MCP server. MCP logs must redact tokens and PII even if tracer would not.

Downstream propagation: gateway adds `x-correlation-id` on the outbound request. Services that include tracer read it again.

---

## 7. MDMS dependencies

Allow-list only these master names. Cache with Caffeine. Unknown master names must be rejected.

| Module | Masters verified in service code | Used for |
|---|---|---|
| `RAINMAKER-PGR` | `ServiceDefs` | complaint type, department, `slaHours` |
| `PropertyTax` | `PropertyType`, `PropertySubType`, `UsageCategory`, `UsageCategoryMajor`, `UsageCategoryMinor`, `UsageCategoryDetail`, `UsageCategorySubMinor`, `OccupancyType`, `ConstructionType`, `ConstructionSubType`, `OwnerShipCategory`, `SubOwnerShipCategory`, `OwnerType`, `MutationReason` | property validation. Search v1 only needs them if filters expose them |
| `egf-master` | `FinancialYear` | property, not required for search v1 |
| `Advertisement` | `CalculationType`, `TaxAmount` | fee. Voice-bot also names `AdType`, `Location`, `FaceArea`, `NightLight` |
| `CHB` | `Purpose`, `SpecialCategory`, `CommunityHalls`, `HallCode`, `Parks`, `ParkCode`, `GuestHouses`, `GuestHouseCode`, `Stadiums`, `StadiumCode`, `Crematoriums`, `CrematoriumCode`, `VenueType`, `Documents`, `CalculationType` | venue booking |
| `Request-Service` | `WaterTankerCalculationType`, `TankerDeliveryTimeCalculationType`, `MobileToiletCalculationType` | request service |
| tree pruning | `TreePruningCalculationType` | tp-services |
| `common-masters` | referenced, individual master names not fully listed | address/boundary style data |
| BillingService | `BusinessService` (UI payment rules) | part-payment rules on the payment page, not an MCP calculation |

**UNKNOWN:** the MDMS data files (the actual rows) are not in this repository. Role-to-action rows are also absent. See section 10.

---

## 8. Workflow dependencies

| Module | businessService | Citizen-relevant actions verified in code or UI | Employee actions (out of v1) |
|---|---|---|---|
| PGR | `PGR` | Create timeline uses `APPLY`. Constants also include `REOPEN`, `RATE`, `COMMENT` | `ASSIGN`, `RESOLVE`, `REJECT`, `REASSIGN`, and composite names in `PGRConstants` |
| Property | `PT.CREATE`, `PT.LEGACY`, `PT.MUTATION` | not used by v1 search | employee actions not in scope |
| Advertisement | billing name `adv-services` | create/update exist; action string **UNKNOWN** | employee update out of scope |
| Community hall | billing `chb-services`; refund workflow `booking-refund` | cancel sets `bookingStatus=CANCELLED`; constant `MOVETOEMPLOYEE`; payment sets action `PAY` | employee workflow out of scope |
| Water tanker | `watertanker` | `workflow.action` required; value **UNKNOWN** | `APPROVE`, `REJECT`, `PAY`, `RATE` |
| Mobile toilet | `mobileToilet` | same gap | same constants file |
| Tree pruning | `treePruning` | `workflow` required; documents required | `PAY` |

Status for get_status should come from the module record first:

- PGR `applicationStatus` plus workflow timeline when the module does not include it
- Property `status`
- Bookings `bookingStatus`

SLA: PGR master field `slaHours` is verified as the MDMS keyword. Do not invent an SLA value when `ServiceDefs` has no row. Other modules: **UNKNOWN** whether a citizen SLA is returned on search.

---

## 9. Billing dependencies

All bill reads go through gateway `POST /billing-service/bill/v2/_fetchbill`.

| Consumer | `businessService` query value | `consumerCode` |
|---|---|---|
| Property tax | `PT` | property id |
| Advertisement | `adv-services` | booking number (UI payment path uses `bookingNo`) |
| Community hall | `chb-services` | `bookingNo` |
| Water tanker | `watertanker` | **UNKNOWN** whether the bill consumer code is `bookingNo` |
| Mobile toilet | `mobileToilet` | **UNKNOWN** |
| Tree pruning | `treePruning` | **UNKNOWN** |

Collection payment creation (`/collection-services/payments/_create`) is out of scope. The only collection call that v1 cancel might need is `/collection-services/payments/CHB/_workflow`, and only to pass through a refund workflow the UI already performs. That call must not be designed until the collection contract is read.

---

## 10. Role-action checklist

**BLOCKER.** This repository does not contain MDMS `ACCESSCONTROL-ACTIONS` or role-action rows for the municipal APIs. Finance SQL migrations under `finance/egov/**/roleaction*.sql` are the legacy ERP schema, not the DIGIT MDMS actions for PGR, property, advertisement, or booking.

What is verified:

- Authorization endpoint: `POST /access/v1/actions/_authorize`
- Action lookup used by the UI: `/access/v1/actions/mdms/_get` (`urls.js` `access_control`)
- The gateway sends the **full request path** as `uri`

What is not verified: role code, action id, and the exact action URL string stored in MDMS for each operation.

`docs/role-action-checklist.md` lists the paths that must be matched. It does not invent roles.

MCP `allowedRolesHint` may be filled only after those rows are supplied. Until then, `list_services` should return the descriptor catalog and state that gateway authorization is authoritative. Hiding a service because of a guessed role would be a defect.

---

## 11. Compatibility issues

### 11.1 Tracer

**VERIFIED compatible** with Java 17 and Spring Boot 3.2.2. Do not upgrade it.

### 11.2 MCP stack versus Spring Boot 3.2

**BLOCKER for dependency selection.**

| Option | What was verified | Fit with Boot 3.2.2 |
|---|---|---|
| Spring AI 1.1.x `spring-ai-starter-mcp-server-webmvc` | Project states 1.1.x targets Spring Boot 3.5.x. Streamable HTTP is `spring.ai.mcp.server.protocol=STREAMABLE` on that starter. Java 17+ is supported. | Not compatible. Boot 3.5 ≠ 3.2 |
| Spring AI 1.0 GA docs | Getting started note: Spring Boot 3.4.x and 3.5.x | Not 3.2 |
| Spring AI 2.x | Targets Spring Boot 4.x. Spring transports moved to `org.springframework.ai:mcp-spring-webmvc` | Not 3.2 |
| MCP Java SDK `io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.18.4` | Declares `spring-webmvc` 6.2.1 and Jakarta Servlet 6.1. Boot 3.2.2 uses Spring Framework 6.1.x | Declared dependency is newer than Boot 3.2.2. Do not force Spring 6.2 onto a Boot 3.2 app |

Sources checked on 2026-10-06:

- https://github.com/spring-projects/spring-ai/tree/refs/heads/1.1.x
- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
- https://java.sdk.modelcontextprotocol.io/latest/quickstart/
- Maven Central POM for `io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.18.4`

**UNKNOWN:** whether an older `mcp-spring-webmvc` (before 0.18) was built against Spring 6.1. That has to be a version spike, not a guess in the POM.

**Recommended resolution (pick one before coding):**

1. Keep the MCP server on Boot 3.2.2 and use `io.modelcontextprotocol.sdk:mcp-core` plus a servlet Streamable HTTP transport whose POM imports Spring 6.1. STDIO can be a dev profile on the same core library. This preserves the “Boot 3.2.x” rule.
2. Allow **only this new service** to use Spring Boot 3.4 or 3.5 so `spring-ai-starter-mcp-server-webmvc` with `protocol=STREAMABLE` can be used. Do not upgrade pgr-services, property-services, or tracer. The MCP server would not embed tracer; it would copy the correlation header contract.

Option 1 matches the brief more closely. Option 2 is the smaller integration with the official starter. Both keep downstream calls on the existing gateway.

### 11.3 Other libraries

Resilience4j, Micrometer, Jackson, JsonPath, Jakarta Validation, Caffeine, and JUnit 5 are compatible with Boot 3.2. OpenTelemetry instrumentation should follow the Boot 3.2 line, not a Boot 3.5 BOM. No extra framework is required.

### 11.4 Redis

Gateway already uses Redis for rate limiting. Single-use confirmation tokens and idempotency need a shared store. If Redis is down, the server must refuse writes. An in-memory map must not be described as single-use.

---

## 12. Missing information

| Gap | Why it matters | Needed from |
|---|---|---|
| Deployed gateway routes for property, billing, advertisement, CHB, request, tree pruning, collection | Checked-in `routes.properties` will not forward those paths | Environment gateway ConfigMap or Helm values |
| MDMS role-action rows (role, action id, action URL) | Cannot honestly fill `allowedRolesHint` or the checklist | MDMS data for the target tenant |
| Which MDMS generation is live (`egov-mdms-service` vs `mdms-v2`) | PGR complaint types | Environment flag `upyog.mdms.v2.enabled` and data |
| MCP client auth header contract | How the assistant passes the existing UPYOG token | Chatbot team |
| `CommonUtils` tenant extraction rules | TenantValidator must match the gateway | Read during implementation; do not simplify early |
| Empty `_fetchbill` JSON | `hasPendingBill: false` mapping | Contract test against billing-service or a captured response |
| Advertisement and CHB create without slot search / estimate | Write descriptors may be unsafe | Read create service methods before phase 3 |
| Collection `payments/{businessService}/_workflow` schema | CHB cancel refund path | `collection-services` controller |
| Citizen workflow `action` string for advertisement, CHB create, water tanker, mobile toilet, tree pruning | prepare_action must send the same action the UI sends | UI submit payload or workflow MDMS |
| Water tanker / mobile toilet / tree pruning bill `consumerCode` | payment link for those modules | A captured `_fetchbill` or demand row |
| UI base URL per environment | payment link host | Deployment config, not a hard-coded host |
| Verified UI deep links for document steps | v1 must not invent them | Frontend route table pass during phase 3 |
| Idempotency support downstream | Create APIs inspected do not show an idempotency-key header | Confirm in persister/DB unique keys before choosing shared dedup |
| Redis availability in the MCP namespace | Token single-use and rate limit | Platform |

---

## 13. Risks

1. Calling services by cluster DNS (`http://property-services:8080`) would bypass gateway RBAC. Forbidden.
2. Trusting agent-supplied `RequestInfo` would let the model set `uuid` and roles. Gateway replaces `userInfo` only after it reads `authToken`. A forged token still fails `/_details`. A forged `userInfo` with a valid token is overwritten. The MCP server must still strip these fields so they never appear in audit logs or prompts.
3. CHB cancel plus collection refund is two writes. A descriptor template that only sets `bookingStatus` would diverge from the UI for online payments.
4. Tree pruning create requires `documentDetails`. Shipping it as a normal create will fail or encourage a fake file-store id.
5. PGR, advertisement, and booking search can return mobile numbers, names, and addresses. Masking has to run before the tool result is returned.
6. Tracer-style body logging would store tokens and citizen text. MCP logging must not enable that.
7. `routes.properties` route 18 is duplicated. Do not treat that file as a complete production route list.
8. Billing response field `ResposneInfo` is misspelled. Clients that require `ResponseInfo` will not see it. The MCP projector must read `Bill` and tolerate the misspelled info field.
9. Property citizen search injects the user’s mobile number when criteria are empty. The MCP search tool should pass an explicit property id or mobile only when the user asked, and should still let the service apply its own rule.
10. Spring AI Boot mismatch. Picking the latest starter and then forcing Boot 3.5 “because it compiles” would violate the stack rule without a recorded decision.

---

## 14. Recommended implementation sequence

Do not start this sequence until the review accepts this report and chooses the MCP library option in section 11.2.

**Commit 1 — this report (not committed yet).** Discovery only.

**Commit 2 — foundation and one read.** Boot 3.2.2 unless option 2 is approved. Streamable HTTP. STDIO only in a dev profile. Gateway client, server-built `RequestInfo`, correlation id (`x-correlation-id` / MDC `CORRELATION_ID`), audit skeleton with redaction, descriptor load and startup validation, and PGR search end to end through the gateway path. WireMock contract test for that one operation.

**Commit 3 — remaining reads and billing.** Property search, advertisement search, venue search, `lookup_master` for the allow-listed masters, `get_status` from module fields, `get_pending_bill`, `get_payment_link` using `/upyog-ui/citizen/payment/my-bills/{businessService}/{consumerCode}`.

**Commit 4 — writes.** `prepare_action` and `confirm_action` for PGR create, advertisement create, venue create, and venue cancel. Redis-backed HMAC token (5-minute TTL, configurable). No write on prepare. Cancel must include the collection refund pass-through only after that contract is verified. Tree pruning create stays blocked on documents.

**Commit 5 — hardening.** Rate limits, per-service circuit breakers, timeouts (read ~8s, write ~20s), no write retries, contract tests, the evaluation set in `docs/chatbot-team-guide.md` turned into executable checks, Docker and Kubernetes aligned with existing UPYOG deployment docs, README.

---

## 15. Decisions recorded

The open choices from the first review are closed in **Review decisions** at the top of this file.

Still not called live, because no environment base URL or token was provided in this session:

- `POST /access/v1/actions/mdms/_get` — implement the client against `ActionRequest` / `ActionSearchResponse`. Do not invent action ids.
- `POST /billing-service/bill/v2/_fetchbill` — return the `Bill` array from that response.
