# Coverage baseline (JDK 17)
Generated: 2026-10-28T07:54:58Z

## core-smoke-tests
Status: OK (no jacoco.xml — see raw/core-smoke-tests.log)

## egov-idgen
**Module line:** 50.7% (155/306) | **Module branch:** 38.9% (42/108)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.id.service.IdGenerationService` | 50.5 | 40.5 | 91 | 44 |
| `org.egov.id.exception.GlobalExceptionHandler` | 20.0 | 37.5 | 40 | 5 |
| `org.egov.id.service.MdmsService` | 70.6 | 34.6 | 20 | 17 |
| `org.egov.id.api.IdGenerationController` | 100.0 | 100.0 | 0 | 0 |

## egov-indexer
**Module line:** 12.0% (130/1080) | **Module branch:** 13.0% (56/432)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.infra.indexer.util.IndexerUtils` | 11.4 | 11.8 | 273 | 97 |
| `org.egov.infra.indexer.service.DataTransformationService` | 10.3 | 8.7 | 113 | 42 |
| `org.egov.infra.indexer.service.LegacyIndexService$1` | 0.0 | 0.0 | 94 | 24 |
| `org.egov.infra.indexer.service.ReindexService` | 0.0 | 0.0 | 67 | 14 |
| `org.egov.infra.indexer.service.ReindexService$1` | 0.0 | 0.0 | 61 | 14 |
| `org.egov.infra.indexer.bulkindexer.BulkIndexer` | 1.7 | 0.0 | 57 | 10 |
| `org.egov.infra.indexer.service.LegacyIndexService` | 0.0 | 0.0 | 50 | 6 |
| `org.egov.infra.indexer.models.AuditDetails` | 0.0 | 0.0 | 48 | 16 |
| `org.egov.infra.indexer.consumer.PGRCustomIndexMessageListener` | 0.0 | 0.0 | 20 | 4 |
| `org.egov.infra.indexer.util.IndexerUtils$1` | 0.0 | 0.0 | 19 | 4 |
| `org.egov.infra.indexer.models.IndexJob` | 0.0 | 0.0 | 15 | 94 |
| `org.egov.infra.indexer.validator.Validator` | 54.5 | 38.9 | 15 | 11 |
| `org.egov.infra.indexer.consumer.ReindexMessageListener` | 0.0 | 0.0 | 15 | 2 |
| `org.egov.infra.indexer.consumer.LegacyIndexMessageListener` | 0.0 | 0.0 | 15 | 2 |
| `org.egov.infra.indexer.controller.IndexerController` | 0.0 | 100.0 | 14 | 0 |
| `org.egov.infra.indexer.service.ReindexService$2` | 0.0 | 0.0 | 12 | 2 |
| `org.egov.infra.indexer.models.IndexJob$StatusEnum` | 0.0 | 0.0 | 12 | 4 |
| `org.egov.infra.indexer.consumer.PTCustomIndexMessageListener` | 0.0 | 0.0 | 12 | 2 |
| `org.egov.infra.indexer.consumer.BPACustomIndexMessageListener` | 0.0 | 100.0 | 10 | 0 |
| `org.egov.infra.indexer.consumer.CoreIndexMessageListener` | 0.0 | 100.0 | 8 | 0 |
| `org.egov.infra.indexer.producer.IndexerProducer` | 0.0 | 100.0 | 6 | 0 |
| `org.egov.infra.indexer.models.IndexJobWrapper` | 0.0 | 0.0 | 6 | 22 |
| `org.egov.infra.indexer.service.IndexerService` | 92.9 | 78.6 | 4 | 6 |
| `org.egov.infra.indexer.models.IndexJob$IndexJobBuilder` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.indexer.models.AuditDetails$AuditDetailsBuilder` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.indexer.models.IndexJobWrapper$IndexJobWrapperBuilder` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.indexer.util.IndexerConstants` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.indexer.util.ResponseInfoFactory` | 100.0 | 100.0 | 0 | 0 |

## egov-notification-sms
Status: BUILD_FAILED (no jacoco.xml — see raw/egov-notification-sms.log)

## egov-pg-service
**Module line:** 5.7% (245/4277) | **Module branch:** 1.6% (34/2151)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.pg.service.gateways.paygov.PayGovGateway` | 0.0 | 0.0 | 335 | 55 |
| `org.egov.pg.service.gateways.nttdata.NttdataGateway` | 0.0 | 0.0 | 306 | 60 |
| `org.egov.pg.service.EnrichmentService` | 0.6 | 0.0 | 172 | 20 |
| `org.egov.pg.service.gateways.axis.AxisGateway` | 0.0 | 0.0 | 161 | 44 |
| `org.egov.pg.service.gateways.payu.PayuGateway` | 0.0 | 0.0 | 141 | 28 |
| `org.egov.pg.service.gateways.razorpay.RazorpayGateway` | 0.0 | 0.0 | 119 | 12 |
| `org.egov.pg.service.UserService` | 0.0 | 0.0 | 106 | 24 |
| `org.egov.pg.service.gateways.icici.ICICIGateway` | 0.0 | 0.0 | 98 | 12 |
| `org.egov.pg.service.NotificationService` | 0.0 | 0.0 | 95 | 72 |
| `org.egov.pg.service.PaymentsService` | 1.1 | 0.0 | 90 | 20 |
| `org.egov.pg.service.gateways.phonepe.PhonepeGateway` | 0.0 | 0.0 | 90 | 4 |
| `org.egov.pg.web.controllers.RedirectController` | 0.0 | 0.0 | 82 | 62 |
| `org.egov.pg.service.RefundService` | 0.0 | 0.0 | 77 | 14 |
| `org.egov.pg.utils.NotificationUtil` | 0.0 | 0.0 | 73 | 26 |
| `org.egov.pg.service.gateways.paytm.PaytmGateway` | 0.0 | 0.0 | 70 | 4 |
| `org.egov.pg.service.gateways.nttdata.AuthEncryption` | 0.0 | 100.0 | 61 | 0 |
| `org.egov.pg.repository.TransactionRowMapper` | 6.2 | 0.0 | 60 | 14 |
| `org.egov.pg.service.gateways.axis.AxisUtils` | 0.0 | 0.0 | 56 | 28 |
| `org.egov.pg.service.gateways.nttdata.PayDetails` | 0.0 | 100.0 | 53 | 0 |
| `org.egov.pg.repository.TransactionQueryBuilder` | 0.0 | 0.0 | 51 | 26 |
| `org.egov.pg.service.gateways.nttdata.AipayService` | 0.0 | 100.0 | 50 | 0 |
| `org.egov.pg.service.gateways.nttdata.Hex` | 0.0 | 0.0 | 50 | 10 |
| `org.egov.pg.service.gateways.nttdata.AtomEncryption` | 0.0 | 0.0 | 46 | 6 |
| `org.egov.pg.service.GatewayService` | 2.4 | 0.0 | 41 | 18 |
| `org.egov.pg.validator.RefundValidator` | 0.0 | 0.0 | 39 | 38 |
| `org.egov.pg.service.gateways.nttdata.PayInstrument` | 0.0 | 100.0 | 37 | 0 |
| `org.egov.pg.models.CollectionPayment` | 0.0 | 0.0 | 37 | 184 |
| `org.egov.pg.repository.RefundTransactionQueryBuilder` | 0.0 | 0.0 | 36 | 20 |
| `org.egov.pg.service.gateways.icici.ICICIUtils` | 0.0 | 0.0 | 34 | 8 |
| `org.egov.pg.models.Bill` | 41.4 | 0.0 | 34 | 202 |
| `org.egov.pg.models.BillDetail` | 37.3 | 0.0 | 32 | 18 |
| `org.egov.pg.repository.RefundTransactionRowMapper` | 0.0 | 0.0 | 31 | 2 |
| `org.egov.pg.service.gateways.nttdata.MerchDetails` | 0.0 | 100.0 | 30 | 0 |
| `org.egov.pg.service.gateways.paygov.PayGovUtils` | 0.0 | 0.0 | 29 | 12 |
| `org.egov.pg.models.BillAccountDetail` | 0.0 | 0.0 | 29 | 102 |
| `org.egov.pg.service.gateways.nttdata.HeadDetails` | 0.0 | 100.0 | 27 | 0 |
| `org.egov.pg.repository.PgDetailRepository` | 0.0 | 0.0 | 27 | 2 |
| `org.egov.pg.repository.BankAccountRepository` | 0.0 | 0.0 | 26 | 2 |
| `org.egov.pg.models.BusinessDetailsRequestInfo` | 0.0 | 0.0 | 26 | 166 |
| `org.egov.pg.service.jobs.earlyReconciliation.EarlyRefundScheduler` | 0.0 | 0.0 | 25 | 4 |

## egov-user
**Module line:** 10.3% (391/3787) | **Module branch:** 7.0% (103/1474)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.user.persistence.repository.UserRepository` | 0.2 | 0.0 | 454 | 236 |
| `org.egov.user.web.controller.CustomAuthenticationController` | 0.0 | 0.0 | 361 | 122 |
| `org.egov.user.domain.service.UserService` | 43.4 | 36.4 | 226 | 117 |
| `org.egov.user.domain.service.utils.EncryptionDecryptionUtil` | 0.4 | 0.0 | 224 | 168 |
| `org.egov.user.security.oauth2.custom.authproviders.CustomAuthenticationProvider` | 0.0 | 0.0 | 187 | 80 |
| `org.egov.user.persistence.repository.AddressRepository` | 0.6 | 0.0 | 159 | 62 |
| `org.egov.user.domain.service.TokenService` | 0.6 | 0.0 | 156 | 58 |
| `org.egov.user.security.oauth2.custom.CustomOpaqueTokenGenerator` | 0.0 | 0.0 | 131 | 30 |
| `org.egov.user.web.controller.UserController` | 25.1 | 33.3 | 125 | 32 |
| `org.egov.user.repository.builder.UserTypeQueryBuilder` | 0.0 | 0.0 | 116 | 74 |
| `org.egov.user.repository.rowmapper.UserResultSetExtractorV2` | 0.0 | 0.0 | 82 | 34 |
| `org.egov.user.repository.rowmapper.UserResultSetExtractor` | 0.0 | 0.0 | 79 | 34 |
| `org.egov.user.security.AuthorizationServerConfiguration` | 0.0 | 0.0 | 74 | 2 |
| `org.egov.user.persistence.repository.AuditRepository` | 0.0 | 0.0 | 68 | 30 |
| `org.egov.user.persistence.repository.RoleRepository` | 0.0 | 0.0 | 66 | 18 |
| `org.egov.user.web.adapters.errors.UserRequestErrorAdapter` | 0.0 | 0.0 | 65 | 18 |
| `org.egov.user.security.oauth2.custom.CustomTokenEnhancer` | 0.0 | 0.0 | 54 | 8 |
| `org.egov.user.domain.service.EnhancedRedisOAuth2AuthorizationService` | 0.0 | 0.0 | 48 | 18 |
| `org.egov.user.web.errorhandlers.ErrorHandler` | 0.0 | 0.0 | 44 | 8 |
| `org.egov.user.repository.builder.AddressQueryBuilder` | 0.0 | 0.0 | 41 | 18 |
| `org.egov.user.security.oauth2.custom.authproviders.CustomPreAuthenticatedProvider` | 0.0 | 0.0 | 40 | 14 |
| `org.egov.user.EgovUserApplication` | 0.0 | 100.0 | 38 | 0 |
| `org.egov.user.security.oauth2.custom.CustomAuthenticationManager` | 0.0 | 0.0 | 37 | 18 |
| `org.egov.user.web.adapters.errors.UserUpdateErrorAdapter` | 0.0 | 0.0 | 35 | 6 |
| `org.egov.user.domain.service.RedisOAuth2AuthorizationService` | 0.0 | 0.0 | 32 | 12 |
| `org.egov.user.security.CustomAuthenticationKeyGenerator` | 0.0 | 0.0 | 29 | 24 |
| `org.egov.user.repository.rowmapper.UserRowMapper` | 0.0 | 0.0 | 27 | 20 |
| `org.egov.user.security.oauth2.custom.CustomOpaqueRefreshTokenGenerator` | 0.0 | 0.0 | 24 | 4 |
| `org.egov.user.repository.rowmapper.DigilockerUserResultSetExtractor` | 0.0 | 0.0 | 23 | 4 |
| `org.egov.user.domain.service.utils.NotificationUtil` | 0.0 | 100.0 | 21 | 0 |
| `org.egov.user.persistence.repository.ActionRestRepository` | 0.0 | 0.0 | 21 | 2 |
| `org.egov.user.web.controller.LogoutController` | 0.0 | 0.0 | 20 | 6 |
| `org.egov.user.persistence.dto.Action` | 0.0 | 100.0 | 19 | 0 |
| `org.egov.user.persistence.repository.OtpRepository` | 5.9 | 0.0 | 16 | 4 |
| `org.egov.user.web.adapters.errors.InvalidAccessTokenErrorHandler` | 0.0 | 100.0 | 14 | 0 |
| `org.egov.user.web.adapters.errors.UserNotFoundErrorHandler` | 0.0 | 100.0 | 14 | 0 |
| `org.egov.user.web.adapters.errors.InvalidLoggedInUserUpdatePasswordRequestErrorHandler` | 76.3 | 80.0 | 14 | 2 |
| `org.egov.user.web.adapters.errors.UserNameNotValidErrorHandler` | 0.0 | 100.0 | 14 | 0 |
| `org.egov.user.web.adapters.errors.InvalidUserSearchRequestErrorHandler` | 0.0 | 100.0 | 14 | 0 |
| `org.egov.user.web.adapters.errors.AtleastOneRoleCodeErrorHandler` | 0.0 | 100.0 | 14 | 0 |

## egov-workflow-v2
**Module line:** 69.5% (1342/1930) | **Module branch:** 50.6% (337/666)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.wf.util.WorkflowUtil` | 17.0 | 9.5 | 127 | 67 |
| `org.egov.wf.repository.querybuilder.WorkflowQueryBuilder` | 68.8 | 64.0 | 101 | 54 |
| `org.egov.wf.validator.WorkflowValidator` | 21.7 | 9.0 | 72 | 71 |
| `org.egov.wf.repository.WorKflowRepository` | 49.1 | 35.0 | 56 | 13 |
| `org.egov.wf.repository.BusinessServiceRepository` | 50.0 | 22.2 | 39 | 28 |
| `org.egov.wf.util.EscalationUtil` | 48.4 | 50.0 | 32 | 12 |
| `org.egov.wf.service.EnrichmentService` | 87.4 | 67.9 | 32 | 27 |
| `org.egov.wf.service.UserService` | 52.6 | 25.0 | 27 | 9 |
| `org.egov.wf.service.MDMSService` | 67.7 | 0.0 | 21 | 2 |
| `org.egov.wf.service.TransitionService` | 72.2 | 40.5 | 20 | 25 |
| `org.egov.wf.validator.BusinessServiceValidator` | 83.5 | 75.0 | 15 | 7 |
| `org.egov.wf.Main` | 0.0 | 100.0 | 11 | 0 |
| `org.egov.wf.util.ResponseInfoFactory` | 0.0 | 0.0 | 11 | 10 |
| `org.egov.wf.repository.ServiceRequestRepository` | 33.3 | 100.0 | 10 | 0 |
| `org.egov.wf.service.BusinessMasterService` | 75.0 | 100.0 | 8 | 0 |
| `org.egov.wf.web.controllers.BusinessServiceController` | 75.0 | 100.0 | 5 | 0 |
| `org.egov.wf.util.WorkflowConstants` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.wf.util.BusinessUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.wf.service.StatusUpdateService` | 100.0 | 100.0 | 0 | 0 |
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
**Module line:** 51.4% (407/792) | **Module branch:** 47.2% (154/326)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `com.example.gateway.filters.pre.helpers.UpyogMcpGatewayAuthHelper` | 0.0 | 0.0 | 31 | 4 |
| `com.example.gateway.filters.pre.helpers.RbacFilterHelper` | 64.1 | 26.5 | 28 | 25 |
| `com.example.gateway.filters.post.CustomAsyncFilter` | 0.0 | 0.0 | 25 | 2 |
| `com.example.gateway.filters.pre.helpers.CorrIdFormDataFilterHelper` | 0.0 | 0.0 | 24 | 12 |
| `com.example.gateway.filters.pre.AuthPreCheckFilter` | 0.0 | 0.0 | 23 | 10 |
| `com.example.gateway.filters.pre.RbacFilter` | 0.0 | 0.0 | 22 | 12 |
| `com.example.gateway.filters.pre.AuthFilter` | 0.0 | 0.0 | 21 | 12 |
| `com.example.gateway.filters.pre.helpers.RequestEnrichmentFilterHelper` | 68.3 | 68.2 | 20 | 7 |
| `com.example.gateway.filters.pre.helpers.PreHookFilterHelper` | 0.0 | 100.0 | 19 | 0 |
| `com.example.gateway.filters.pre.CorrelationIdFilter` | 0.0 | 0.0 | 19 | 8 |
| `com.example.gateway.filters.pre.RbacPreCheckFilter` | 0.0 | 0.0 | 19 | 12 |
| `com.example.gateway.filters.pre.helpers.CorrelationIdFilterHelper` | 75.0 | 47.6 | 16 | 22 |
| `com.example.gateway.filters.pre.RequestEnrichmentFilter` | 0.0 | 0.0 | 14 | 8 |
| `com.example.gateway.utils.UserUtils` | 60.0 | 0.0 | 12 | 4 |
| `com.example.gateway.GatewayApplication` | 0.0 | 100.0 | 12 | 0 |
| `com.example.gateway.filters.post.ResponseEnhancementFilter` | 0.0 | 100.0 | 11 | 0 |
| `com.example.gateway.filters.post.PostEventLogFilter` | 0.0 | 0.0 | 11 | 8 |
| `com.example.gateway.filters.pre.helpers.RbacPreCheckFormDataFilterHelper` | 0.0 | 0.0 | 11 | 4 |
| `com.example.gateway.filters.pre.PreHookFilter` | 0.0 | 0.0 | 11 | 6 |
| `com.example.gateway.ratelimiters.RateLimiterConfiguration` | 0.0 | 100.0 | 11 | 0 |
| `com.example.gateway.utils.ExceptionUtils` | 85.5 | 82.4 | 8 | 6 |
| `com.example.gateway.filters.error.ErrorFilter` | 0.0 | 100.0 | 6 | 0 |
| `com.example.gateway.filters.pre.RequestStartTimeFilter` | 0.0 | 100.0 | 5 | 0 |
| `com.example.gateway.filters.post.PostHookFilter` | 0.0 | 100.0 | 4 | 0 |
| `com.example.gateway.filters.post.CustomAsyncFilter$1` | 0.0 | 100.0 | 1 | 0 |
| `com.example.gateway.constants.GatewayConstants` | 0.0 | 100.0 | 1 | 0 |
| `com.example.gateway.filters.pre.helpers.AuthCheckFilterHelper` | 100.0 | 80.0 | 0 | 2 |
| `com.example.gateway.filters.pre.helpers.RbacPreCheckFilterHelper` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.filters.pre.helpers.AuthPreCheckFilterHelper` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.utils.UpyogMcpGatewaySupport` | 100.0 | 92.9 | 0 | 1 |
| `com.example.gateway.utils.EventLoggerUtil` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.utils.ExceptionUtils$1` | 100.0 | 100.0 | 0 | 0 |
| `com.example.gateway.utils.CommonUtils` | 100.0 | 90.5 | 0 | 4 |
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
**Module line:** 26.6% (256/962) | **Module branch:** 27.9% (72/258)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.infra.mdms.service.impl.ThemeConfigServiceImpl` | 0.0 | 0.0 | 100 | 46 |
| `org.egov.infra.mdms.service.impl.WorkflowServiceImpl` | 0.0 | 0.0 | 65 | 12 |
| `org.egov.infra.mdms.repository.impl.ThemeConfigRepositoryImpl` | 0.0 | 0.0 | 62 | 18 |
| `org.egov.infra.mdms.service.MDMSServiceV2` | 0.0 | 0.0 | 52 | 8 |
| `org.egov.infra.mdms.service.SchemaDefinitionService` | 1.9 | 0.0 | 51 | 6 |
| `org.egov.infra.mdms.repository.querybuilder.MdmsDataQueryBuilderV2` | 0.0 | 0.0 | 48 | 20 |
| `org.egov.infra.mdms.repository.querybuilder.SchemaDefinitionQueryBuilder` | 0.0 | 0.0 | 35 | 16 |
| `org.egov.infra.mdms.repository.impl.MdmsDataRepositoryImpl` | 0.0 | 0.0 | 32 | 4 |
| `org.egov.infra.mdms.controller.ThemeConfigController` | 0.0 | 100.0 | 31 | 0 |
| `org.egov.infra.mdms.repository.querybuilder.MdmsDataQueryBuilder` | 0.0 | 0.0 | 28 | 10 |
| `org.egov.infra.mdms.repository.rowmapper.MdmsDataRowMapperV2` | 0.0 | 0.0 | 26 | 4 |
| `org.egov.infra.mdms.repository.rowmapper.SchemaDefinitionRowMapper` | 0.0 | 0.0 | 25 | 6 |
| `org.egov.infra.mdms.repository.rowmapper.MdmsDataRowMapper` | 0.0 | 0.0 | 23 | 6 |
| `org.egov.infra.mdms.repository.impl.SchemaDefinitionDbRepositoryImpl` | 0.0 | 100.0 | 19 | 0 |
| `org.egov.infra.mdms.utils.FallbackUtil` | 51.5 | 62.5 | 16 | 6 |
| `org.egov.infra.mdms.controller.SchemaDefinitionController` | 0.0 | 100.0 | 16 | 0 |
| `org.egov.infra.mdms.utils.SchemaUtil` | 6.2 | 0.0 | 15 | 2 |
| `org.egov.infra.mdms.service.enrichment.SchemaDefinitionEnricher` | 0.0 | 0.0 | 15 | 4 |
| `org.egov.infra.mdms.controller.MDMSControllerV2` | 0.0 | 100.0 | 14 | 0 |
| `org.egov.infra.mdms.utils.QueryUtil` | 64.9 | 50.0 | 13 | 5 |
| `org.egov.infra.mdms.service.validator.SchemaDefinitionValidator` | 68.0 | 31.2 | 8 | 11 |
| `org.egov.MDMSApplication` | 0.0 | 100.0 | 3 | 0 |
| `org.egov.infra.mdms.producer.Producer` | 0.0 | 100.0 | 3 | 0 |
| `org.egov.infra.mdms.service.enrichment.MdmsDataEnricher` | 90.5 | 83.3 | 2 | 1 |
| `org.egov.infra.mdms.errors.ErrorCodes` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.mdms.utils.MDMSConstants` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.mdms.constants.SchemaDefinitionConstant` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.mdms.service.SchemaDefinitionService$SchemaDefinitionServiceBuilder` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.infra.mdms.service.validator.MdmsDataValidator` | 100.0 | 97.1 | 0 | 1 |
| `org.egov.infra.mdms.utils.CompositeUniqueIdentifierGenerationUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.utils.ErrorUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.utils.ResponseUtil` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.controller.MDMSController` | 100.0 | 100.0 | 0 | 0 |
| `org.egov.infra.mdms.service.MDMSService` | 100.0 | 100.0 | 0 | 0 |

## tenant
Status: OK (no jacoco.xml — see raw/tenant.log)

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
**Module line:** 2.2% (21/949) | **Module branch:** 0.0% (0/290)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `org.egov.receipt.consumer.service.VoucherServiceImpl` | 0.0 | 0.0 | 294 | 120 |
| `org.egov.receipt.consumer.util.PaymentUtils` | 0.0 | 0.0 | 150 | 18 |
| `org.egov.receipt.consumer.service.EgfKafkaListener` | 0.0 | 0.0 | 135 | 58 |
| `org.egov.mdms.service.MicroServiceUtilImpl` | 0.0 | 0.0 | 118 | 58 |
| `org.egov.receipt.consumer.service.InstrumentService` | 0.0 | 0.0 | 81 | 16 |
| `org.egov.receipt.consumer.repository.ServiceRequestRepository` | 0.0 | 0.0 | 48 | 8 |
| `org.egov.mdms.service.TokenService` | 0.0 | 0.0 | 37 | 2 |
| `org.egov.receipt.consumer.service.InstrumentUpdateConsumer` | 0.0 | 0.0 | 16 | 10 |
| `org.egov.FinanceCollectionsVoucherConsumerApplication` | 0.0 | 100.0 | 15 | 0 |
| `org.egov.receipt.custom.exception.VoucherCustomException` | 0.0 | 100.0 | 8 | 0 |
| `org.egov.receipt.consumer.service.ReceiptService` | 0.0 | 100.0 | 7 | 0 |
| `org.egov.receipt.consumer.repository.VoucherIntegartionLogRepository` | 0.0 | 100.0 | 7 | 0 |
| `org.egov.receipt.consumer.entity.VoucherIntegrationLog` | 76.9 | 100.0 | 3 | 0 |
| `org.egov.receipt.consumer.repository.builder.VoucherIntegrationLogQueryBuilder` | 91.7 | 100.0 | 1 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$1` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$3` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$2` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$5` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$4` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$7` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.mdms.service.MicroServiceUtilImpl$6` | 0.0 | 100.0 | 1 | 0 |
| `org.egov.mdms.service.TokenService$1` | 0.0 | 100.0 | 1 | 0 |

## dashboard-analytics
**Module line:** 0.9% (25/2655) | **Module branch:** 0.6% (7/1158)

| Class | Line % | Branch % | Missed lines | Missed branches |
| --- | ---: | ---: | ---: | ---: |
| `com.tarento.analytics.dao.impl.ElasticSearchDaoImpl` | 0.0 | 0.0 | 574 | 316 |
| `com.tarento.analytics.handler.LineChartResponseHandler` | 0.6 | 0.0 | 179 | 108 |
| `com.tarento.analytics.org.service.TarentoServiceImpl` | 0.0 | 0.0 | 139 | 98 |
| `com.tarento.analytics.handler.MetricChartResponseHandler` | 0.8 | 0.0 | 123 | 104 |
| `com.tarento.analytics.handler.AdvanceTableChartResponseHandler` | 0.8 | 0.0 | 119 | 72 |
| `com.tarento.analytics.service.impl.MetadataServiceImpl` | 0.0 | 0.0 | 101 | 26 |
| `com.tarento.analytics.service.impl.QueryServiceImpl` | 0.0 | 0.0 | 87 | 34 |
| `com.tarento.analytics.org.service.MdmsApiMappings` | 0.0 | 0.0 | 76 | 36 |
| `com.tarento.analytics.handler.TableChartResponseHandler` | 1.4 | 0.0 | 68 | 32 |
| `com.tarento.analytics.org.service.MdmsServiceImpl` | 0.0 | 0.0 | 66 | 34 |
| `com.tarento.analytics.handler.PerformanceChartResponeHandler` | 1.6 | 0.0 | 62 | 36 |
| `com.tarento.analytics.service.impl.RestService` | 0.0 | 0.0 | 58 | 6 |
| `com.tarento.analytics.controllers.DashboardController` | 0.0 | 0.0 | 56 | 8 |
| `com.tarento.analytics.helper.TargetPerDateComputeHelper` | 0.0 | 0.0 | 55 | 36 |
| `com.tarento.analytics.dto.Tenant` | 0.0 | 100.0 | 53 | 0 |
| `com.tarento.analytics.handler.MetricsInsightsHandler` | 0.0 | 0.0 | 45 | 26 |
| `com.tarento.analytics.utils.ResponseGenerator` | 0.0 | 0.0 | 44 | 4 |
| `com.tarento.analytics.org.service.MdmsService` | 0.0 | 0.0 | 44 | 4 |
| `com.tarento.analytics.dto.City` | 0.0 | 100.0 | 44 | 0 |
| `com.tarento.analytics.handler.IResponseHandler` | 4.3 | 0.0 | 44 | 28 |
| `com.tarento.analytics.service.impl.AmazonS3ClientServiceImpl` | 0.0 | 0.0 | 36 | 2 |
| `com.tarento.analytics.handler.PieChartResponseHandler` | 2.8 | 0.0 | 35 | 10 |
| `com.tarento.analytics.dto.AggregateRequestDtoV3` | 0.0 | 100.0 | 31 | 0 |
| `com.tarento.analytics.repository.ElasticSearchRepository` | 0.0 | 0.0 | 29 | 16 |
| `com.tarento.analytics.dto.Data` | 0.0 | 100.0 | 27 | 0 |
| `com.tarento.analytics.ConfigurationLoader` | 0.0 | 0.0 | 26 | 4 |
| `com.tarento.analytics.dto.Plot` | 0.0 | 100.0 | 25 | 0 |
| `com.tarento.analytics.handler.TablePostResponseHandler` | 3.8 | 0.0 | 25 | 14 |
| `com.tarento.analytics.helper.AverageComputedField` | 0.0 | 0.0 | 23 | 4 |
| `com.tarento.analytics.helper.PercentageComputedField` | 0.0 | 0.0 | 23 | 12 |
| `com.tarento.analytics.helper.AdditiveComputedField` | 0.0 | 0.0 | 21 | 6 |
| `com.tarento.analytics.helper.ComputedFieldHelper` | 0.0 | 0.0 | 20 | 8 |
| `com.tarento.analytics.helper.SubtractiveComputedField` | 0.0 | 0.0 | 20 | 4 |
| `com.tarento.analytics.utils.ResponseCode` | 0.0 | 0.0 | 20 | 4 |
| `com.tarento.analytics.org.service.ClientService` | 0.0 | 0.0 | 20 | 16 |
| `com.tarento.analytics.dto.InsightsWidget` | 0.0 | 100.0 | 19 | 0 |
| `com.tarento.analytics.AnalyticApp` | 0.0 | 100.0 | 19 | 0 |
| `com.tarento.analytics.utils.ResponseRecorder` | 0.0 | 0.0 | 18 | 10 |
| `com.tarento.analytics.dto.Tenants` | 0.0 | 100.0 | 15 | 0 |
| `com.tarento.analytics.dto.RequestDate` | 0.0 | 100.0 | 13 | 0 |

