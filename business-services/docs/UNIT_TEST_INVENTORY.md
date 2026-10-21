# business-services unit test inventory

Auto-generated inventory: every `src/main/java` type and whether `<ClassName>Test` or `<ClassName>IT` exists.

| Module | Total classes | Has test | Missing | Notes |
|--------|---------------|----------|---------|-------|
| billing-service | 141 | 139 | 2 | see detail below |

## billing-service

- **Total:** 141 | **With test:** 139 | **Without dedicated test:** 2

### Interface Use Impl Test (1)

- `org.egov.demand.validation.SanitizeHtml` (interface)

### Abstract Use Subclass Test (1)

- `org.egov.demand.helper.JsonIgnoreHelper` (abstract)

### Tested (139)

_139 classes — omitted for brevity._

| collection-services | 198 | 189 | 9 | see detail below |

## collection-services

- **Total:** 198 | **With test:** 189 | **Without dedicated test:** 9

### Missing Test (1)

- `org.egov.collection.web.controller.ReceiptControllerV2` (class)

### Interface Use Impl Test (1)

- `org.egov.collection.validation.SanitizeHtml` (interface)

### Not Testable Commented (7)

- `org.egov.collection.model.ReceiptCommonModel` (class)
- `org.egov.collection.repository.CollectionRepository` (class)
- `org.egov.collection.repository.querybuilder.CollectionsQueryBuilder` (class)
- `org.egov.collection.repository.rowmapper.CollectionResultSetExtractor` (class)
- `org.egov.collection.service.CollectionService` (class)
- `org.egov.collection.service.WorkflowService` (class)
- `org.egov.collection.util.ReceiptEnricher` (class)

### Tested (189)

_189 classes — omitted for brevity._

| dashboard-analytics | 97 | 85 | 12 | see detail below |

## dashboard-analytics

- **Total:** 97 | **With test:** 85 | **Without dedicated test:** 12

### Interface Use Impl Test (12)

- `com.tarento.analytics.dao.ElasticSearchDao` (interface)
- `com.tarento.analytics.handler.IPostResponseHandler` (interface)
- `com.tarento.analytics.handler.IResponseHandler` (interface)
- `com.tarento.analytics.handler.InsightsHandler` (interface)
- `com.tarento.analytics.helper.ComputeHelper` (interface)
- `com.tarento.analytics.helper.IComputedField` (interface)
- `com.tarento.analytics.org.service.ClientService` (interface)
- `com.tarento.analytics.service.AmazonS3ClientService` (interface)
- `com.tarento.analytics.service.MetadataService` (interface)
- `com.tarento.analytics.service.QueryService` (interface)
- `com.tarento.analytics.utils.ElasticProperties` (interface)
- `com.tarento.analytics.utils.PathRoutes` (interface)

### Tested (85)

_85 classes — omitted for brevity._

| dashboard-data-engine | 74 | 70 | 4 | see detail below |

## dashboard-data-engine

- **Total:** 74 | **With test:** 70 | **Without dedicated test:** 4

### Interface Use Impl Test (4)

- `org.upyog.dashboard.client.DashboardFeignClient` (interface)
- `org.upyog.dashboard.client.UserFeignClient` (interface)
- `org.upyog.dashboard.service.BulkIngestionInitService` (interface)
- `org.upyog.dashboard.service.IngestionRecordPersistenceService` (interface)

### Tested (70)

_70 classes — omitted for brevity._

| dashboard-data-extractor | 52 | 48 | 4 | see detail below |

## dashboard-data-extractor

- **Total:** 52 | **With test:** 48 | **Without dedicated test:** 4

### Missing Test (1)

- `org.upyog.dashboard.controller.IngestionTestController` (class)

### Interface Use Impl Test (3)

- `org.upyog.dashboard.extractor.LegacyBatchExtractor` (interface)
- `org.upyog.dashboard.extractor.ModuleExtractor` (interface)
- `org.upyog.dashboard.service.IngestionPersistenceService` (interface)

### Tested (48)

- `org.upyog.dashboard.DashboardDataExtractorApplication` (class)
- `org.upyog.dashboard.chb.constants.CHBDatabaseConstants` (class)
- `org.upyog.dashboard.chb.mapper.CHBRowMapper` (class)
- `org.upyog.dashboard.chb.model.RawChbMetric` (class)
- `org.upyog.dashboard.config.AppConfig` (class)
- `org.upyog.dashboard.config.DashboardExtractorProperties` (class)
- `org.upyog.dashboard.config.SchemaMappingConfig` (class)
- `org.upyog.dashboard.config.ShedLockConfig` (class)
- `org.upyog.dashboard.constants.DashboardExtractorConstants` (class)
- `org.upyog.dashboard.controller.LegacyIngestionController` (class)
- `org.upyog.dashboard.controller.TenantController` (class)
- `org.upyog.dashboard.entity.DailyIngestionData` (class)
- `org.upyog.dashboard.entity.LegacyIngestionData` (class)
- `org.upyog.dashboard.enums.IngestionStatus` (enum)
- `org.upyog.dashboard.extractor.impl.ChbModuleExtractor` (class)
- `org.upyog.dashboard.extractor.impl.GenericLegacyBatchExtractor` (class)
- `org.upyog.dashboard.extractor.impl.PgrModuleExtractor` (class)
- `org.upyog.dashboard.extractor.impl.PtModuleExtractor` (class)
- `org.upyog.dashboard.mdms.client.MdmsClient` (class)
- `org.upyog.dashboard.mdms.model.MasterDetail` (class)
- `org.upyog.dashboard.mdms.model.MdmsCriteria` (class)
- `org.upyog.dashboard.mdms.model.MdmsCriteriaReq` (class)
- `org.upyog.dashboard.mdms.model.MdmsTenantResponse` (class)
- `org.upyog.dashboard.mdms.model.ModuleDetail` (class)
- `org.upyog.dashboard.model.IngestionModuleDetail` (class)
- `org.upyog.dashboard.model.IngestionSchedulerDetail` (class)
- `org.upyog.dashboard.pgr.constants.PGRDatabaseConstants` (class)
- `org.upyog.dashboard.pgr.mapper.PGRRowMapper` (class)
- `org.upyog.dashboard.pgr.model.RawPgrMetric` (class)
- `org.upyog.dashboard.pt.constants.PTDatabaseConstants` (class)
- `org.upyog.dashboard.pt.mapper.PTRowmapper` (class)
- `org.upyog.dashboard.pt.model.RawPtCollection` (class)
- `org.upyog.dashboard.pt.model.RawPtMetric` (class)
- `org.upyog.dashboard.registry.ExtractorRegistry` (class)
- `org.upyog.dashboard.repository.IngestionSummaryRepository` (class)
- `org.upyog.dashboard.repository.querybuilder.IngestionSummaryQueryBuilder` (class)
- `org.upyog.dashboard.scheduler.DailyIngestionScheduler` (class)
- `org.upyog.dashboard.scheduler.LegacyIngestionScheduler` (class)
- `org.upyog.dashboard.service.DailyIngestionService` (class)
- `org.upyog.dashboard.service.LegacyBatchIngestRequest` (class)
- `org.upyog.dashboard.service.LegacyBatchIngestionOrchestrator` (class)
- `org.upyog.dashboard.service.LegacyIngestionService` (class)
- `org.upyog.dashboard.service.TenantSyncService` (class)
- `org.upyog.dashboard.service.impl.JdbcIngestionPersistenceServiceImpl` (class)
- `org.upyog.dashboard.service.impl.KafkaIngestionPersistenceServiceImpl` (class)
- `org.upyog.dashboard.util.DatabaseQueryExecutor` (class)
- `org.upyog.dashboard.util.ExtractorUtil` (class)
- `org.upyog.dashboard.util.HierarchyParser` (class)

| dashboard-ingest | 68 | 57 | 11 | see detail below |

## dashboard-ingest

- **Total:** 68 | **With test:** 57 | **Without dedicated test:** 11

### Interface Use Impl Test (11)

- `com.ingestpipeline.config.DomainConfig` (interface)
- `com.ingestpipeline.consumer.KafkaConsumer` (interface)
- `com.ingestpipeline.repository.TargetDataDao` (interface)
- `com.ingestpipeline.service.DigressService` (interface)
- `com.ingestpipeline.service.EnrichmentService` (interface)
- `com.ingestpipeline.service.IESService` (interface)
- `com.ingestpipeline.service.IngestService` (interface)
- `com.ingestpipeline.service.TransformService` (interface)
- `com.ingestpipeline.service.ValidationService` (interface)
- `com.ingestpipeline.util.Constants` (interface)
- `com.ingestpipeline.util.ResponseMessages` (interface)

### Tested (57)

_57 classes — omitted for brevity._

| egf-instrument | 96 | 96 | 0 | see detail below |

## egf-instrument

- **Total:** 96 | **With test:** 96 | **Without dedicated test:** 0

### Tested (96)

_96 classes — omitted for brevity._

| egf-master | 378 | 373 | 5 | see detail below |

## egf-master

- **Total:** 378 | **With test:** 373 | **Without dedicated test:** 5

### Interface Use Impl Test (3)

- `org.egov.common.domain.annotation.Unique` (interface)
- `org.egov.egf.master.domain.model.EntityType` (interface)
- `org.egov.egf.master.domain.repository.CommonDbRepository` (interface)

### Abstract Use Subclass Test (2)

- `org.egov.common.persistence.repository.ESRepository` (abstract)
- `org.egov.common.persistence.repository.JdbcRepository` (abstract)

### Tested (373)

_373 classes — omitted for brevity._

| egov-apportion-service | 38 | 36 | 2 | see detail below |

## egov-apportion-service

- **Total:** 38 | **With test:** 36 | **Without dedicated test:** 2

### Interface Use Impl Test (2)

- `org.egov.service.Apportion` (interface)
- `org.egov.service.ApportionV2` (interface)

### Tested (36)

- `org.egov.ApportionApp` (class)
- `org.egov.config.ApportionConfig` (class)
- `org.egov.config.SwaggerConfiguration` (class)
- `org.egov.producer.Producer` (class)
- `org.egov.repository.ServiceRequestRepository` (class)
- `org.egov.service.ApportionService` (class)
- `org.egov.service.ApportionServiceV2` (class)
- `org.egov.service.MDMSService` (class)
- `org.egov.service.TaxHeadMasterService` (class)
- `org.egov.service.TranslationService` (class)
- `org.egov.service.apportions.OrderByPriority` (class)
- `org.egov.service.apportions.OrderByPriorityApportion` (class)
- `org.egov.util.ApportionConstants` (class)
- `org.egov.util.ResponseInfoFactory` (class)
- `org.egov.web.controllers.ApportionController` (class)
- `org.egov.web.controllers.ApportionControllerV2` (class)
- `org.egov.web.enums.CollectionType` (enum)
- `org.egov.web.models.ApportionDemandResponse` (class)
- `org.egov.web.models.ApportionRequest` (class)
- `org.egov.web.models.ApportionRequestV2` (class)
- `org.egov.web.models.ApportionResponse` (class)
- `org.egov.web.models.AuditDetails` (class)
- `org.egov.web.models.Bill` (class)
- `org.egov.web.models.BillAccountDetail` (class)
- `org.egov.web.models.BillDetail` (class)
- `org.egov.web.models.Bucket` (class)
- `org.egov.web.models.Demand` (class)
- `org.egov.web.models.DemandDetail` (class)
- `org.egov.web.models.Role` (class)
- `org.egov.web.models.TaxAndPayment` (class)
- `org.egov.web.models.TaxDetail` (class)
- `org.egov.web.models.TaxHeadMaster` (class)
- `org.egov.web.models.enums.Category` (enum)
- `org.egov.web.models.enums.DemandApportionRequest` (class)
- `org.egov.web.models.enums.Purpose` (enum)
- `org.egov.web.models.enums.ReceiptType` (enum)

| egov-hrms | 54 | 51 | 3 | see detail below |

## egov-hrms

- **Total:** 54 | **With test:** 51 | **Without dedicated test:** 3

### Missing Test (1)

- `org.egov.hrms.model.DepartmentalTest` (class)

### Interface Use Impl Test (2)

- `org.egov.hrms.service.UserService` (interface)
- `org.egov.hrms.validation.SanitizeHtml` (interface)

### Tested (51)

_51 classes — omitted for brevity._

| employee-dashboard | 25 | 23 | 2 | see detail below |

## employee-dashboard

- **Total:** 25 | **With test:** 23 | **Without dedicated test:** 2

### Interface Use Impl Test (2)

- `org.upyog.employee.dasboard.repository.ServiceRequestRepository` (interface)
- `org.upyog.employee.dasboard.service.EmployeeDashboardService` (interface)

### Tested (23)

- `org.upyog.employee.dasboard.Main` (class)
- `org.upyog.employee.dasboard.config.MainConfiguration` (class)
- `org.upyog.employee.dasboard.config.SwaggerConfiguration` (class)
- `org.upyog.employee.dasboard.kafka.Consumer` (class)
- `org.upyog.employee.dasboard.kafka.Producer` (class)
- `org.upyog.employee.dasboard.query.constant.DashboardConstants` (class)
- `org.upyog.employee.dasboard.query.constant.DashboardQueryConstant` (class)
- `org.upyog.employee.dasboard.repository.impl.ServiceRequestRepositoryImpl` (class)
- `org.upyog.employee.dasboard.repository.rowMapper.EmployeeDashboardDetailsRowMapper` (class)
- `org.upyog.employee.dasboard.service.impl.EmployeeDashboardServiceImpl` (class)
- `org.upyog.employee.dasboard.util.MdmsUtil` (class)
- `org.upyog.employee.dasboard.util.ResponseInfoUtil` (class)
- `org.upyog.employee.dasboard.web.controllers.EmployeeDashaboardApiController` (class)
- `org.upyog.employee.dasboard.web.models.EmployeeDashboardDetails` (class)
- `org.upyog.employee.dasboard.web.models.EmployeeDashboardRequest` (class)
- `org.upyog.employee.dasboard.web.models.EmployeeDashboardResponse` (class)
- `org.upyog.employee.dasboard.web.models.Error` (class)
- `org.upyog.employee.dasboard.web.models.ErrorRes` (class)
- `org.upyog.employee.dasboard.web.models.ModuleDashboardData` (class)
- `org.upyog.employee.dasboard.web.models.ModuleName` (enum)
- `org.upyog.employee.dasboard.web.models.ResponseInfo` (class)
- `org.upyog.employee.dasboard.web.models.RoleBasedDashboardRequest` (class)
- `org.upyog.employee.dasboard.web.models.RoleBasedDashboardResponse` (class)

| finance-collections-voucher-consumer | 107 | 105 | 2 | see detail below |

## finance-collections-voucher-consumer

- **Total:** 107 | **With test:** 105 | **Without dedicated test:** 2

### Interface Use Impl Test (2)

- `org.egov.mdms.service.MicroServiceUtil` (interface)
- `org.egov.receipt.consumer.service.VoucherService` (interface)

### Tested (105)

_105 classes — omitted for brevity._

| reconciliation-service | 13 | 9 | 4 | see detail below |

## reconciliation-service

- **Total:** 13 | **With test:** 9 | **Without dedicated test:** 4

### Missing Test (1)

- `org.upyog.reconciliation.controller.TestReconciliationController` (class)

### Interface Use Impl Test (3)

- `org.upyog.reconciliation.extractor.MetricDataExtractor` (interface)
- `org.upyog.reconciliation.repository.ReconConfigurationRepository` (interface)
- `org.upyog.reconciliation.repository.ReconExtractionDetailRepository` (interface)

### Tested (9)

- `org.upyog.reconciliation.ReconciliationServiceApplication` (class)
- `org.upyog.reconciliation.extractor.DataExtractorFactory` (class)
- `org.upyog.reconciliation.extractor.ElasticsearchDataExtractor` (class)
- `org.upyog.reconciliation.model.ReconConfiguration` (class)
- `org.upyog.reconciliation.model.ReconExtractionDetail` (class)
- `org.upyog.reconciliation.producer.ReconciliationProducer` (class)
- `org.upyog.reconciliation.service.ReconciliationSchedulerService` (class)
- `org.upyog.reconciliation.validation.ReconciliationValidationEngine` (class)
- `org.upyog.reconciliation.validation.ValidationResult` (class)

| verification-service | 47 | 44 | 3 | see detail below |

## verification-service

- **Total:** 47 | **With test:** 44 | **Without dedicated test:** 3

### Interface Use Impl Test (3)

- `org.upyog.mapper.CommonDetailsMapper` (interface)
- `org.upyog.service.CommonService` (interface)
- `org.upyog.validation.SanitizeHtml` (interface)

### Tested (44)

- `org.upyog.Main` (class)
- `org.upyog.config.MainConfiguration` (class)
- `org.upyog.config.ModuleConfig` (class)
- `org.upyog.config.SwaggerConfiguration` (class)
- `org.upyog.constants.VerificationSearchConstants` (class)
- `org.upyog.mapper.AdvertisementDetailsMapper` (class)
- `org.upyog.mapper.BuildingPlanDetailMapper` (class)
- `org.upyog.mapper.CndDetails` (class)
- `org.upyog.mapper.CommonDetailsMapperFactory` (class)
- `org.upyog.mapper.CommunityHallDetailsMapper` (class)
- `org.upyog.mapper.EwasteDetailsMapper` (class)
- `org.upyog.mapper.FireNocDetailsMapper` (class)
- `org.upyog.mapper.MobileToiletDetails` (class)
- `org.upyog.mapper.PetDetailsMapper` (class)
- `org.upyog.mapper.PropertyDetailsMapper` (class)
- `org.upyog.mapper.StreetVendingDetailsMapper` (class)
- `org.upyog.mapper.TradeLicenseDetails` (class)
- `org.upyog.mapper.TreePruningDetails` (class)
- `org.upyog.mapper.WaterTankerDetails` (class)
- `org.upyog.repository.ServiceRequestRepository` (class)
- `org.upyog.service.CommonServiceImpl` (class)
- `org.upyog.service.UserService` (class)
- `org.upyog.util.CommonDetailUtil` (class)
- `org.upyog.util.MdmsUtil` (class)
- `org.upyog.util.ResponseInfoFactory` (class)
- `org.upyog.util.UserUtil` (class)
- `org.upyog.validation.SanitizeHtmlValidator` (class)
- `org.upyog.web.controllers.VerificationServiceController` (class)
- `org.upyog.web.models.CommonDetails` (class)
- `org.upyog.web.models.CommonModuleRequest` (class)
- `org.upyog.web.models.CommonModuleResponse` (class)
- `org.upyog.web.models.Document` (class)
- `org.upyog.web.models.Error` (class)
- `org.upyog.web.models.ErrorRes` (class)
- `org.upyog.web.models.ModuleSearchCriteria` (class)
- `org.upyog.web.models.ModuleSearchRequest` (class)
- `org.upyog.web.models.RequestInfo` (class)
- `org.upyog.web.models.ResponseInfo` (class)
- `org.upyog.web.models.Role` (class)
- `org.upyog.web.models.Role2` (class)
- `org.upyog.web.models.TenantRole` (class)
- `org.upyog.web.models.User` (class)
- `org.upyog.web.models.UserInfo` (class)
- `org.upyog.web.models.UserSearchRequest` (class)

## Summary

- **Grand total classes:** 1388
- **With dedicated test class:** 1325
- **Without dedicated test class:** 63
