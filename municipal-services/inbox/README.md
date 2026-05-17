# Inbox Service (inbox)

The inbox service is a aggregation service which aggregates data of municipal services and workflow based on given complex search criteria
and returns applications and workflow data in paginated manner. The service also returns the total count matching the search criteria.



### Service Dependencies


- Workflow Service (workflow-v2)
- User Service (user-service)
- Searcher Service (egov-searcher)
- The Municipal service for which inbox configuration is added


### Swagger API Contract

Link to the swagger API contract [YAML](https://editor.swagger.io/?url=https://raw.githubusercontent.com/upyog/UPYOG/master/municipal-services/docs/inbox.yml#!/) and editor link like below


### Postman Collection



## Service Details

The service aggregates data from workflow and configured municipal services, to provide data required for displaying on inbox screen


### API Details


`_search` : This API is used to search inbox application data based on the criteria provided

---

## Refactoring Changes

The original monolithic `InboxService.java` (~1000+ lines) has been refactored into a clean layered architecture.

### What Changed

| Before | After |
|---|---|
| Single `InboxService.java` with all logic | Split into multiple focused classes |
| All module if-else blocks in one method | Each module has its own `ModuleInboxHandler` |
| Hard to add new modules | Add one class in `ModuleHandlers.java` — done |
| Circular dependency on startup | Resolved using `@Lazy` on `InboxService` injection |

### New Files Added

| File | Purpose |
|---|---|
| `service/handler/InboxOrchestrator.java` | Main pipeline coordinator |
| `service/handler/InboxAssembler.java` | Builds final `List<Inbox>` from business objects + process instances |
| `service/handler/StatusCountService.java` | Handles BPA citizen/locality status count + FSM vehicle enrichment |
| `service/handler/ModuleHandlers.java` | All module-specific handlers |
| `service/handler/ModuleHandlerRegistry.java` | Auto-collects all `ModuleInboxHandler` beans via Spring |
| `service/handler/ModuleInboxHandler.java` | Interface that every module handler implements |
| `service/handler/InboxContext.java` | Per-request value object passed through the pipeline |


## Architecture Flow

```
HTTP Request
     │
     ▼
InboxController
     │
     ▼
InboxService.fetchInboxData()                    
     │
     ▼
InboxOrchestrator.fetchInboxData()
     │
     ├── workflowService.getProcessCount()
     ├── workflowService.getNearingSlaProcessCount()
     ├── workflowService.getProcessStatusCount()
     │
     ├── PATH A: No moduleSearchCriteria
     │       └── Workflow → get process instances
     │           └── InboxService.fetchModuleObjectsPublic() → return inboxes
     │
     └── PATH B: moduleSearchCriteria present
             │
             ├── workflowService.getBusinessService()
             ├── workflowService.getActionableStatusesForRole()
             │
             ├── StatusCountService.handleBpaCitizenStatusCount()
             ├── StatusCountService.handleBpaLocalityStatusCount()
             │
             ├── ModuleHandlerRegistry.getHandler(moduleName)
             │       └── handler.fetchCount()
             │       └── handler.fetchApplicationIds()
             │       └── handler.paramsToRemove()
             │
             ├── [WS/SW Billing] BillingAmendmentInboxFilterService
             │
             ├── InboxAssembler.assemble()
             │       ├── [WS/SW]  → ElasticSearch
             │       ├── [Others] → InboxService.fetchModuleObjectsPublic()
             │       ├── workflowService.getProcessInstance()
             │       └── build List<Inbox>
             │
             └── [FSM] StatusCountService.enrichFsmStatusCount()
                     ├── fetchVehicleTripResponsePublic()
                     ├── populateStatusCountMapPublic()
                     ├── fetchVehicleStatusForApplicationPublic()
                     └── aggregateFsmStatuses()
                             │
                             ▼
                        InboxResponse
```

### CHB Example — Which files are involved

When a request comes for `moduleName: community-hall-booking`, here is which file does what:

```
POST /inbox/v1/_search
     │
     ▼
InboxController
     │  reads moduleName = "community-hall-booking"
     ▼
InboxService.java
     │  delegates to InboxOrchestrator
     ▼
InboxOrchestrator.java
     │  calls workflowService for counts and status map
     │  calls ModuleHandlerRegistry → finds CHBModuleHandler
     ▼
ModuleHandlers.java  (CHBModuleHandler)
     │  fetchCount()
     │    → CommunityHallInboxFilterService.java
     │        → egov-searcher → returns totalCount = 3
     │
     │  fetchApplicationIds()
     │    → CommunityHallInboxFilterService.java
     │        → egov-searcher → returns ["CHB-2024-001", "CHB-2024-002", "CHB-2024-003"]
     │    → puts bookingNo = ["CHB-2024-001",...] in moduleSearchCriteria
     │
     │  paramsToRemove() → removes "offset", "status" from moduleSearchCriteria
     ▼
InboxAssembler.java
     │  fetchModuleObjectsPublic()
     │    → InboxService.java → chb-service/_search
     │        → returns 3 booking JSONObjects
     │
     │  getProcessInstance()
     │    → WorkflowService.java → workflow-v2
     │        → returns 3 ProcessInstances
     │
     │  build List<Inbox>
     │    → Inbox { processInstance, businessObject: { bookingNo: "CHB-2024-001" } }
     │    → Inbox { processInstance, businessObject: { bookingNo: "CHB-2024-002" } }
     │    → Inbox { processInstance, businessObject: { bookingNo: "CHB-2024-003" } }
     ▼
InboxResponse {
  totalCount: 3,
  nearingSlaCount: 1,
  statusMap: [ { status: "PENDING", count: 2 }, { status: "APPROVED", count: 1 } ],
  items: [ Inbox, Inbox, Inbox ]
}
```

---

## Supported Modules

| Module | Handler Class | Filter Service |
|---|---|---|
| TL (Trade License) | `TLModuleHandler` | `TLInboxFilterService` |
| BPAREG | `TLModuleHandler` | `TLInboxFilterService` |
| BPA (Building Plan) | `BPAModuleHandler` | `BPAInboxFilterService` |
| PT (Property Tax) | `PTModuleHandler` | `PtInboxFilterService` |
| PTR (Pet Registration) | `PTRModuleHandler` | `PtrInboxFilterService` |
| NOC | `NOCModuleHandler` | `NOCInboxFilterService` |
| CHB (Community Hall) | `CHBModuleHandler` | `CommunityHallInboxFilterService` |
| NDC (No Dues Certificate) | `NDCModuleHandler` | `NDCInboxFilterService` |
| CND (Construction & Demolition) | `CNDModuleHandler` | `CNDInboxFilterService` |
| EWASTE | `EWASTEModuleHandler` | `EwasteInboxFilterService` |
| ASSET | `ASSETModuleHandler` | `AssetInboxFilterService` |
| SV (Street Vending) | `SVModuleHandler` | `StreetVendingInboxFilterService` |
| WT (Water Tanker) | `WTModuleHandler` | `WTInboxFilterService` |
| MT (Mobile Toilet) | `MTModuleHandler` | `MTInboxFilterService` |
| TP (Tree Pruning) | `TPModuleHandler` | `TPInboxFilterService` |
| PGR AI | `PGRAiModuleHandler` | `PGRAiInboxFilterService` |
| Challan | `ChallanModuleHandler` | `ChallanInboxFilterService` |
| WS (Water & Sewerage) | ElasticSearch path | `WSInboxFilterService` |
| SW (Sewerage) | ElasticSearch path | `SWInboxFilterService` |
| FSM | FSM vehicle path | `FSMInboxFilterService` |
| BS WS/SW (Billing Amendment) | Billing path | `BillingAmendmentInboxFilterService` |

---

## How to Add a New Module

Example: **Community Hall Booking (CHB)**

### Step 1 — `util/CommunityHallConstants.java`

```java
public class CommunityHallConstants {
    public static final String CHB = "community-hall-booking";
    public static final String CHB_BOOKING_NO_PARAM = "bookingNo";
}
```

### Step 2 — `service/CommunityHallInboxFilterService.java`

Follow the same pattern as other filter services. Implement:

```java
@Service
public class CommunityHallInboxFilterService {

    public List<String> fetchApplicationNumbersFromSearcher(
            InboxSearchCriteria criteria,
            HashMap<String, String> statusIdNameMap,
            RequestInfo requestInfo) {
        // call searcher and return list of booking numbers
    }

    public Integer fetchApplicationCountFromSearcher(
            InboxSearchCriteria criteria,
            HashMap<String, String> statusIdNameMap,
            RequestInfo requestInfo) {
        // return total count
    }
}
```

### Step 3 — `service/handler/ModuleHandlers.java`

Add at the bottom of the file:

```java
@Slf4j
@Service
class CHBModuleHandler implements ModuleInboxHandler {

    @Autowired
    private CommunityHallInboxFilterService chbService;

    @Override
    public boolean supports(String moduleName) {
        return CHB.equals(moduleName);
    }

    @Override
    public void fetchApplicationIds(InboxContext ctx) {
        List<String> ids = chbService.fetchApplicationNumbersFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
        if (CollectionUtils.isEmpty(ids)) {
            ctx.setSearchResultEmpty(true);
            return;
        }
        ctx.getCriteria().getModuleSearchCriteria()
                .put(CHB_BOOKING_NO_PARAM, ids);
        ctx.addBusinessKeys(ids);
    }

    @Override
    public int fetchCount(InboxContext ctx) {
        return chbService.fetchApplicationCountFromSearcher(
                ctx.getCriteria(), ctx.getStatusIdNameMap(), ctx.getRequestInfo());
    }

    @Override
    public String getApplicationIdParamKey() {
        return CHB_BOOKING_NO_PARAM;
    }

    @Override
    public List<String> paramsToRemove() {
        return List.of(OFFSET_PARAM, STATUS_PARAM);
    }
}
```

### Step 4 — `resources/application.properties`

```properties
egov.service.search.mapping={\
  ...,\
  "community-hall-booking": {\
    "searchPath": "http://chb-service/chb/booking/v1/_search",\
    "dataRoot": "hallsBookingApplication",\
    "businessIdProperty": "bookingNo",\
    "applNosParam": "bookingNos",\
    "applsStatusParam": "applicationStatus"\
  }\
}
```

> `ModuleHandlerRegistry` automatically picks up the new handler via Spring's `@Service` annotation.
> No changes needed in `InboxOrchestrator`, `InboxAssembler`, or `StatusCountService`.

---

## Reference Document

TBD
