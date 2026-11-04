# Untestable without production change

| Module | Class | Reason | Minimal refactor suggestion |
|--------|-------|--------|----------------------------|
| pt-calculator-v2 | `ReceiptService` | Entire `src/main` type is block-commented out | Restore or delete the dead file so tests can target a real type |
| property-services | `PropertyValidator` (duplicate-owner path) | `validateCreateRequest` mutates `owners` via `remove(null)`; immutable lists from Lombok/builder throw before business validation | Accept `List<OwnerInfo>` copy in validator or document mutable list requirement |
| Multiple | `@Configuration` `@PostConstruct` beans | Side effects on load (timezone, HTTP converters) | Extract pure logic to testable `@Component` helpers |
