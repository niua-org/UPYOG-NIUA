# Coverage baseline (JDK 17)
Generated: 2026-10-28T10:22:51Z

## core-smoke-tests
Status: no jacoco.xml after verify-all

## egov-idgen
**Module line:** 94.1% (288/306) | **Module branch:** 78.7% (85/108)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.id.service.IdGenerationService` | 92.9 | 78.4 | 13 | 16 |
| `org.egov.id.service.MdmsService` | 92.6 | 73.1 | 5 | 7 |
| `org.egov.id.api.IdGenerationController` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.id.exception.GlobalExceptionHandler` | 100.0 | 100.0 | 0 | 0 |

## egov-indexer
**Module line:** 82.2% (888/1080) | **Module branch:** 69.4% (300/432)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.infra.indexer.service.ReindexService$1` | 49.2 | 42.9 | 31 | 8 |
| `org.egov.infra.indexer.util.IndexerUtils` | 90.9 | 83.6 | 28 | 18 |
| `org.egov.infra.indexer.service.LegacyIndexService$1` | 71.3 | 54.2 | 27 | 11 |
| `org.egov.infra.indexer.models.AuditDetails` | 54.2 | 56.2 | 22 | 7 |
| `org.egov.infra.indexer.consumer.PGRCustomIndexMessageListener` | 0.0 | 0.0 | 20 | 4 |
| `org.egov.infra.indexer.controller.IndexerController` | 0.0 | 100.0 | 14 | 0 |
| `org.egov.infra.indexer.consumer.PTCustomIndexMessageListener` | 0.0 | 0.0 | 12 | 2 |
| `org.egov.infra.indexer.consumer.BPACustomIndexMessageListener` | 0.0 | 100.0 | 10 | 0 |
| `org.egov.infra.indexer.service.DataTransformationService` | 95.2 | 91.3 | 6 | 4 |
| `org.egov.infra.indexer.producer.IndexerProducer` | 16.7 | 100.0 | 5 | 0 |
| `org.egov.infra.indexer.service.IndexerService` | 92.9 | 78.6 | 4 | 6 |
| `org.egov.infra.indexer.service.ReindexService$2` | 75.0 | 100.0 | 3 | 0 |
| `org.egov.infra.indexer.service.LegacyIndexService` | 96.0 | 100.0 | 2 | 0 |
| `org.egov.infra.indexer.validator.Validator` | 93.9 | 94.4 | 2 | 1 |
| `org.egov.infra.indexer.util.IndexerUtils$1` | 89.5 | 75.0 | 2 | 1 |
| `org.egov.infra.indexer.service.ReindexService` | 98.5 | 78.6 | 1 | 3 |
| `org.egov.infra.indexer.models.IndexJobWrapper` | 83.3 | 40.9 | 1 | 13 |
| `org.egov.infra.indexer.models.IndexJob` | 93.3 | 43.6 | 1 | 53 |
| `org.egov.infra.indexer.util.IndexerConstants` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.indexer.models.IndexJob$IndexJobBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.indexer.models.AuditDetails$AuditDetailsBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.indexer.models.IndexJob$StatusEnum` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.indexer.models.IndexJobWrapper$IndexJobWrapperBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.indexer.bulkindexer.BulkIndexer` | 100.0 | 90.0 | 0 | 1 |
| `org.egov.infra.indexer.consumer.CoreIndexMessageListener` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.indexer.consumer.ReindexMessageListener` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.indexer.consumer.LegacyIndexMessageListener` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.indexer.util.ResponseInfoFactory` | 100.0 | 100.0 | 0 | 0 |

## egov-notification-sms
Status: no jacoco.xml after verify-all

## egov-pg-service
**Module line:** 73.6% (768/1044) | **Module branch:** 65.5% (194/296)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.pg.service.UserService` | 0.9 | 0.0 | 105 | 24 |
| `org.egov.pg.repository.PgDetailRepository` | 0.0 | 0.0 | 27 | 2 |
| `org.egov.pg.repository.BankAccountRepository` | 3.8 | 0.0 | 25 | 2 |
| `org.egov.pg.repository.CollectionsRepository` | 0.0 | 100.0 | 24 | 0 |
| `org.egov.pg.repository.IdGenRepository` | 4.8 | 0.0 | 20 | 2 |
| `org.egov.pg.service.RefundService` | 75.3 | 42.9 | 19 | 8 |
| `org.egov.pg.utils.NotificationUtil` | 75.3 | 61.5 | 18 | 10 |
| `org.egov.pg.consumer.RefundNotificationConsumer` | 0.0 | 0.0 | 14 | 2 |
| `org.egov.pg.repository.ServiceCallRepository` | 7.1 | 100.0 | 13 | 0 |
| `org.egov.pg.service.NotificationService` | 95.8 | 63.9 | 4 | 26 |
| `org.egov.pg.validator.RefundValidator` | 92.3 | 73.7 | 3 | 10 |
| `org.egov.pg.Main` | 33.3 | 100.0 | 2 | 0 |
| `org.egov.pg.service.GatewayService` | 97.6 | 88.9 | 1 | 2 |
| `org.egov.pg.utils.ResponseInfoFactory` | 90.9 | 100.0 | 1 | 0 |
| `org.egov.pg.service.TransactionService` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.pg.service.EnrichmentService` | 100.0 | 70.0 | 0 | 6 |
| `org.egov.pg.service.IdGenService` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.pg.service.PaymentsService` | 100.0 | 90.0 | 0 | 2 |
| `org.egov.pg.web.controllers.TransactionsApiController` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.pg.consumer.NotificationConsumer` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.pg.constants.TransactionAdditionalFields` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.pg.repository.TransactionRepository` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.pg.repository.RefundRepository` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.pg.validator.TransactionValidator` | 100.0 | 85.0 | 0 | 6 |
| `org.egov.pg.utils.Utils` | 100.0 | 100.0 | 0 | 0 |

## egov-user
**Module line:** 91.5% (195/213) | **Module branch:** 60.0% (42/70)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.user.domain.service.TokenService` | 88.5 | 56.9 | 18 | 25 |
| `org.egov.user.domain.service.utils.LocalizationUtil` | 100.0 | 50.0 | 0 | 1 |
| `org.egov.user.persistence.repository.OtpRepository` | 100.0 | 75.0 | 0 | 1 |
| `org.egov.user.web.controller.PasswordController` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.user.web.controller.LogoutController` | 100.0 | 83.3 | 0 | 1 |

## egov-workflow-v2
**Module line:** 73.7% (1422/1930) | **Module branch:** 61.0% (406/666)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.wf.repository.querybuilder.WorkflowQueryBuilder` | 68.8 | 64.0 | 101 | 54 |
| `org.egov.wf.util.WorkflowUtil` | 35.3 | 39.2 | 99 | 45 |
| `org.egov.wf.validator.WorkflowValidator` | 21.7 | 9.0 | 72 | 71 |
| `org.egov.wf.repository.WorKflowRepository` | 49.1 | 35.0 | 56 | 13 |
| `org.egov.wf.repository.BusinessServiceRepository` | 50.0 | 22.2 | 39 | 28 |
| `org.egov.wf.util.EscalationUtil` | 48.4 | 50.0 | 32 | 12 |
| `org.egov.wf.service.UserService` | 52.6 | 25.0 | 27 | 9 |
| `org.egov.wf.service.MDMSService` | 67.7 | 0.0 | 21 | 2 |
| `org.egov.wf.validator.BusinessServiceValidator` | 83.5 | 75.0 | 15 | 7 |
| `org.egov.wf.Main` | 0.0 | 100.0 | 11 | 0 |
| `org.egov.wf.util.ResponseInfoFactory` | 0.0 | 0.0 | 11 | 10 |
| `org.egov.wf.repository.ServiceRequestRepository` | 33.3 | 100.0 | 10 | 0 |
| `org.egov.wf.service.BusinessMasterService` | 75.0 | 100.0 | 8 | 0 |
| `org.egov.wf.web.controllers.BusinessServiceController` | 75.0 | 100.0 | 5 | 0 |
| `org.egov.wf.util.WorkflowConstants` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.wf.util.BusinessUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.service.StatusUpdateService` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.service.TransitionService` | 100.0 | 95.2 | 0 | 2 |
| `org.egov.wf.service.EnrichmentService` | 100.0 | 96.4 | 0 | 3 |
| `org.egov.wf.service.EscalationService` | 100.0 | 87.5 | 0 | 2 |
| `org.egov.wf.service.WorkflowService` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.web.controllers.WorkflowController` | 100.0 | 75.0 | 0 | 1 |
| `org.egov.wf.web.controllers.EscalationController` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.repository.querybuilder.BusinessServiceQueryBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.repository.querybuilder.EscalationQueryBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.producer.Producer` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.repository.rowmapper.WorkflowRowMapper` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.repository.rowmapper.BusinessServiceRowMapper` | 100.0 | 93.8 | 0 | 1 |
| `org.egov.wf.repository.EscalationRepository` | 100.0 | 100.0 | 0 | 0 |

## gateway
**Module line:** 69.6% (551/792) | **Module branch:** 61.0% (199/326)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `com.example.gateway.filters.post.CustomAsyncFilter` | 0.0 | 0.0 | 25 | 2 |
| `com.example.gateway.filters.pre.RbacFilter` | 0.0 | 0.0 | 22 | 12 |
| `com.example.gateway.filters.pre.AuthFilter` | 0.0 | 0.0 | 21 | 12 |
| `com.example.gateway.filters.pre.CorrelationIdFilter` | 0.0 | 0.0 | 19 | 8 |
| `com.example.gateway.filters.pre.helpers.CorrelationIdFilterHelper` | 75.0 | 52.4 | 16 | 20 |
| `com.example.gateway.filters.pre.helpers.RequestEnrichmentFilterHelper` | 77.8 | 77.3 | 14 | 5 |
| `com.example.gateway.filters.pre.RequestEnrichmentFilter` | 0.0 | 0.0 | 14 | 8 |
| `com.example.gateway.filters.pre.helpers.RbacFilterHelper` | 83.3 | 50.0 | 13 | 17 |
| `com.example.gateway.GatewayApplication` | 0.0 | 100.0 | 12 | 0 |
| `com.example.gateway.filters.post.ResponseEnhancementFilter` | 0.0 | 100.0 | 11 | 0 |
| `com.example.gateway.filters.post.PostEventLogFilter` | 0.0 | 0.0 | 11 | 8 |
| `com.example.gateway.filters.pre.helpers.UpyogMcpGatewayAuthHelper` | 64.5 | 50.0 | 11 | 2 |
| `com.example.gateway.filters.pre.PreHookFilter` | 0.0 | 0.0 | 11 | 6 |
| `com.example.gateway.ratelimiters.RateLimiterConfiguration` | 0.0 | 100.0 | 11 | 0 |
| `com.example.gateway.utils.ExceptionUtils` | 85.5 | 82.4 | 8 | 6 |
| `com.example.gateway.filters.error.ErrorFilter` | 0.0 | 100.0 | 6 | 0 |
| `com.example.gateway.filters.pre.RequestStartTimeFilter` | 0.0 | 100.0 | 5 | 0 |
| `com.example.gateway.filters.post.PostHookFilter` | 0.0 | 100.0 | 4 | 0 |
| `com.example.gateway.filters.pre.AuthPreCheckFilter` | 82.6 | 50.0 | 4 | 5 |
| `com.example.gateway.filters.post.CustomAsyncFilter$1` | 0.0 | 100.0 | 1 | 0 |
| `com.example.gateway.filters.pre.RbacPreCheckFilter` | 94.7 | 66.7 | 1 | 4 |
| `com.example.gateway.constants.GatewayConstants` | 0.0 | 100.0 | 1 | 0 |
| `com.example.gateway.filters.pre.helpers.CorrIdFormDataFilterHelper` | 100.0 | 83.3 | 0 | 2 |
| `com.example.gateway.filters.pre.helpers.AuthCheckFilterHelper` | 100.0 | 80.0 | 0 | 2 |
| `com.example.gateway.filters.pre.helpers.RbacPreCheckFilterHelper` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.filters.pre.helpers.PreHookFilterHelper` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.filters.pre.helpers.AuthPreCheckFilterHelper` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.filters.pre.helpers.RbacPreCheckFormDataFilterHelper` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.utils.UpyogMcpGatewaySupport` | 100.0 | 92.9 | 0 | 1 |
| `com.example.gateway.utils.EventLoggerUtil` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.utils.ExceptionUtils$1` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.utils.CommonUtils` | 100.0 | 90.5 | 0 | 4 |
| `com.example.gateway.utils.UserUtils` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.utils.URLProvider` | 100.0 | 75.0 | 0 | 3 |
| `com.example.gateway.producer.Producer` | 100.0 | 100.0 | 0 | 0 |

## gis-service
**Module line:** 76.9% (293/381) | **Module branch:** 63.9% (78/122)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.gis.adapters.AssetServiceAdapter` | 69.8 | 50.0 | 45 | 33 |
| `org.egov.gis.repository.ServiceRequestRepository` | 6.7 | 100.0 | 14 | 0 |
| `org.egov.gis.models.Entity` | 43.8 | 100.0 | 9 | 0 |
| `org.egov.gis.adapters.PropertyServiceAdapter` | 92.9 | 82.5 | 8 | 7 |
| `org.egov.gis.models.GeometryType` | 0.0 | 100.0 | 6 | 0 |
| `org.egov.gis.GisServiceApplication` | 0.0 | 100.0 | 3 | 0 |
| `org.egov.gis.service.GisService` | 91.9 | 66.7 | 3 | 4 |
| `org.egov.gis.service.ServiceRegistry` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.gis.models.GisRequest$GisRequestBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.gis.models.GisResponse$GisResponseBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.gis.models.Entity$EntityBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.gis.web.controller.GisController` | 100.0 | 100.0 | 0 | 0 |

## mdms-v2
**Module line:** 87.2% (767/880) | **Module branch:** 69.8% (169/242)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.infra.mdms.repository.impl.ThemeConfigRepositoryImpl` | 54.8 | 27.8 | 28 | 13 |
| `org.egov.infra.mdms.service.impl.ThemeConfigServiceImpl` | 75.0 | 50.0 | 25 | 23 |
| `org.egov.infra.mdms.utils.QueryUtil` | 64.9 | 50.0 | 13 | 5 |
| `org.egov.infra.mdms.controller.ThemeConfigController` | 64.5 | 100.0 | 11 | 0 |
| `org.egov.infra.mdms.repository.querybuilder.MdmsDataQueryBuilder` | 67.9 | 50.0 | 9 | 5 |
| `org.egov.infra.mdms.repository.querybuilder.MdmsDataQueryBuilderV2` | 81.2 | 85.0 | 9 | 3 |
| `org.egov.infra.mdms.service.validator.SchemaDefinitionValidator` | 68.0 | 31.2 | 8 | 11 |
| `org.egov.infra.mdms.repository.querybuilder.SchemaDefinitionQueryBuilder` | 91.4 | 68.8 | 3 | 5 |
| `org.egov.infra.mdms.service.impl.WorkflowServiceImpl` | 96.9 | 66.7 | 2 | 4 |
| `org.egov.infra.mdms.service.enrichment.MdmsDataEnricher` | 90.5 | 83.3 | 2 | 1 |
| `org.egov.infra.mdms.service.SchemaDefinitionService` | 98.1 | 100.0 | 1 | 0 |
| `org.egov.infra.mdms.service.SchemaDefinitionService$SchemaDefinitionServiceBuilder` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.mdms.utils.MDMSConstants` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.mdms.service.MDMSServiceV2` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.service.MDMSService` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.service.validator.MdmsDataValidator` | 100.0 | 97.1 | 0 | 1 |
| `org.egov.infra.mdms.repository.impl.MdmsDataRepositoryImpl` | 100.0 | 75.0 | 0 | 1 |
| `org.egov.infra.mdms.repository.impl.SchemaDefinitionDbRepositoryImpl` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.service.enrichment.SchemaDefinitionEnricher` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.utils.SchemaUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.utils.CompositeUniqueIdentifierGenerationUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.utils.ErrorUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.utils.FallbackUtil` | 100.0 | 93.8 | 0 | 1 |
| `org.egov.infra.mdms.utils.ResponseUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.controller.SchemaDefinitionController` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.controller.MDMSController` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.controller.MDMSControllerV2` | 100.0 | 100.0 | 0 | 0 |

## tenant
**Module line:** 95.7% (444/464) | **Module branch:** 85.7% (48/56)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.tenant.web.adapter.TenantCreateRequestErrorAdapter` | 92.9 | 92.3 | 6 | 2 |
| `org.egov.tenant.web.controller.TenantController` | 73.7 | 0.0 | 5 | 2 |
| `org.egov.tenant.persistence.entity.Tenant` | 92.5 | 100.0 | 4 | 0 |
| `org.egov.EgovTenantApplication` | 80.0 | 100.0 | 2 | 0 |
| `org.egov.tenant.persistence.entity.City` | 94.6 | 100.0 | 2 | 0 |
| `org.egov.tenant.domain.service.TenantService` | 95.2 | 100.0 | 1 | 0 |
| `org.egov.tenant.persistence.rowmapper.CityRowMapper` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.rowmapper.TenantRowMapper` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.domain.service.TenantService$1` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.domain.exception.InvalidTenantDetailsException` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.domain.exception.TenantInvalidCodeException` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.domain.exception.DuplicateTenantCodeException` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.EgovTenantApplication$1` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.web.adapter.DuplicateTenantCodeErrorAdapter` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.web.adapter.TenantCodeErrorAdapter` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.entity.City$CityBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.entity.Tenant$TenantBuilder` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.TenantRepository$4` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.TenantRepository$3` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.TenantRepository$2` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.TenantRepository$1` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.CityRepository$1` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.CityRepository$3` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.CityRepository$2` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.TenantRepository` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.CityRepository` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.tenant.persistence.repository.MdmsRepository` | 100.0 | 77.8 | 0 | 4 |
| `org.egov.tenant.web.controller.TenantControllerAdvice` | 100.0 | 100.0 | 0 | 0 |

## upyog-draft-service
**Module line:** 95.2% (315/331) | **Module branch:** 83.3% (100/120)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.upyog.draft.DraftApplication` | 0.0 | 100.0 | 12 | 0 |
| `org.upyog.draft.repository.DraftRepository` | 90.3 | 83.3 | 3 | 1 |
| `org.upyog.draft.service.DraftService` | 99.4 | 78.0 | 1 | 18 |
| `org.upyog.draft.repository.rowmapper.DraftRowMapper$1` | 100.0 | 100.0 | 0 | 0 |
| `org.upyog.draft.repository.rowmapper.DraftRowMapper` | 100.0 | 100.0 | 0 | 0 |
| `org.upyog.draft.web.controllers.DraftController` | 100.0 | 100.0 | 0 | 0 |
| `org.upyog.draft.scheduler.DraftCleanupScheduler` | 100.0 | 100.0 | 0 | 0 |
| `org.upyog.draft.repository.query.DraftQueryBuilder` | 100.0 | 96.2 | 0 | 1 |
| `org.upyog.draft.producer.Producer` | 100.0 | 100.0 | 0 | 0 |

## finance-collections-voucher-consumer
**Module line:** 92.5% (878/949) | **Module branch:** 71.7% (208/290)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.mdms.service.MicroServiceUtilImpl` | 85.6 | 58.6 | 17 | 24 |
| `org.egov.receipt.consumer.service.VoucherServiceImpl` | 94.9 | 76.7 | 15 | 28 |
| `org.egov.FinanceCollectionsVoucherConsumerApplication` | 0.0 | 100.0 | 15 | 0 |
| `org.egov.receipt.consumer.service.EgfKafkaListener` | 93.3 | 69.0 | 9 | 18 |
| `org.egov.receipt.consumer.repository.ServiceRequestRepository` | 91.7 | 87.5 | 4 | 1 |
| `org.egov.receipt.custom.exception.VoucherCustomException` | 50.0 | 100.0 | 4 | 0 |
| `org.egov.receipt.consumer.service.InstrumentService` | 96.3 | 68.8 | 3 | 5 |
| `org.egov.receipt.consumer.entity.VoucherIntegrationLog` | 76.9 | 100.0 | 3 | 0 |
| `org.egov.receipt.consumer.repository.builder.VoucherIntegrationLogQueryBuilder` | 91.7 | 100.0 | 1 | 0 |
| `org.egov.receipt.consumer.service.InstrumentUpdateConsumer` | 100.0 | 60.0 | 0 | 4 |
| `org.egov.receipt.consumer.service.ReceiptService` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.receipt.consumer.repository.VoucherIntegartionLogRepository` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.receipt.consumer.util.PaymentUtils` | 100.0 | 88.9 | 0 | 2 |
| `org.egov.mdms.service.TokenService` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$1` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$3` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$2` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$5` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$4` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$7` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$6` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.mdms.service.TokenService$1` | 100.0 | 100.0 | 0 | 0 |

## dashboard-analytics
**Module line:** 84.3% (253/300) | **Module branch:** 65.8% (79/120)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `com.tarento.analytics.handler.TableChartResponseHandler` | 88.4 | 62.5 | 8 | 12 |
| `com.tarento.analytics.helper.PercentageComputedField` | 65.2 | 33.3 | 8 | 8 |
| `com.tarento.analytics.handler.PieChartResponseHandler` | 80.6 | 30.0 | 7 | 7 |
| `com.tarento.analytics.utils.JSONObjectUtil` | 30.0 | 100.0 | 7 | 0 |
| `com.tarento.analytics.helper.SubtractiveComputedField` | 75.0 | 25.0 | 5 | 3 |
| `com.tarento.analytics.utils.ResponseRecorder` | 72.2 | 60.0 | 5 | 4 |
| `com.tarento.analytics.helper.AdditiveComputedField` | 85.7 | 83.3 | 3 | 1 |
| `com.tarento.analytics.helper.ComputedFieldFactory` | 90.0 | 87.5 | 1 | 1 |
| `com.tarento.analytics.utils.JsonKey` | 0.0 | 100.0 | 1 | 0 |
| `com.tarento.analytics.utils.ElasticProperties$Query` | 0.0 | 100.0 | 1 | 0 |
| `com.tarento.analytics.service.impl.QueryServiceImpl` | 98.9 | 85.3 | 1 | 5 |
| `com.tarento.analytics.helper.ComputeHelperFactory` | 100.0 | 100.0 | 0 | 0 |

