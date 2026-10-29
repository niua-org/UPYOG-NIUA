package org.egov.tl.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.TLRepository;
import org.egov.tl.service.notification.EditNotificationService;
import org.egov.tl.util.TLConstants;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.validator.TLValidator;
import org.egov.tl.web.models.OwnerInfo;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseDetail;
import org.egov.tl.web.models.TradeLicenseRequest;
import org.egov.tl.web.models.TradeLicenseSearchCriteria;
import org.egov.tl.web.models.TradeUnit;
import org.egov.tl.web.models.user.UserDetailResponse;
import org.egov.tl.workflow.ActionValidator;
import org.egov.tl.workflow.TLWorkflowService;
import org.egov.tl.workflow.WorkflowIntegrator;
import org.egov.tl.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.egov.tl.util.TLConstants.STATUS_REJECTED;
import static org.egov.tl.util.TLConstants.businessService_BPA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeLicenseServiceTest {

    @Mock private WorkflowIntegrator wfIntegrator;
    @Mock private EnrichmentService enrichmentService;
    @Mock private UserService userService;
    @Mock private TLRepository repository;
    @Mock private ActionValidator actionValidator;
    @Mock private TLValidator tlValidator;
    @Mock private TLWorkflowService tlWorkflowService;
    @Mock private CalculationService calculationService;
    @Mock private TradeUtil util;
    @Mock private DiffService diffService;
    @Mock private TLConfiguration config;
    @Mock private EditNotificationService editNotificationService;
    @Mock private WorkflowService workflowService;
    @Mock private TradeUtil tradeUtil;
    @Mock private TLBatchService tlBatchService;

    @InjectMocks
    private TradeLicenseService tradeLicenseService;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        requestInfo = RequestInfo.builder().build();
        when(config.getApplicationValidity()).thenReturn("365");
    }

    @Test
    void countLicenses_whenRepositoryReturnsCount_returnsValue() {
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(repository.getLicenseCount(criteria)).thenReturn(7);

        int count = tradeLicenseService.countLicenses(criteria, requestInfo, TLConstants.businessService_TL, new HttpHeaders());

        assertThat(count).isEqualTo(7);
        verify(enrichmentService).enrichSearchCriteriaWithAccountId(eq(requestInfo), eq(criteria));
    }

    @Test
    void countApplications_delegatesToRepository() {
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        Map<String, Integer> expected = Map.of("NEW", 2);
        when(repository.getApplicationsCount(criteria)).thenReturn(expected);

        assertThat(tradeLicenseService.countApplications(criteria, requestInfo, TLConstants.businessService_TL, new HttpHeaders()))
                .isEqualTo(expected);
    }

    @Test
    void getLicensesWithOwnerInfo_whenRepositoryEmpty_returnsEmptyList() {
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(repository.getLicenses(criteria, criteria.getTenantId())).thenReturn(Collections.emptyList());

        assertThat(tradeLicenseService.getLicensesWithOwnerInfo(criteria, requestInfo)).isEmpty();
    }

    @Test
    void getLicensesWithOwnerInfo_whenRowsExist_enrichesFromUserService() {
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        TradeLicense license = TradeLicense.builder().id("tl-1").businessService(TLConstants.businessService_TL).build();
        when(repository.getLicenses(criteria, criteria.getTenantId())).thenReturn(List.of(license));
        when(enrichmentService.enrichTradeLicenseSearch(any(), eq(criteria), eq(requestInfo))).thenReturn(List.of(license));

        List<TradeLicense> result = tradeLicenseService.getLicensesWithOwnerInfo(criteria, requestInfo);

        assertThat(result).containsExactly(license);
    }

    @Test
    void plainSearch_whenNoIds_returnsEmptyWithoutRepositoryLookup() {
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(repository.fetchTradeLicenseIds(criteria)).thenReturn(Collections.emptyList());

        assertThat(tradeLicenseService.plainSearch(criteria, requestInfo)).isEmpty();
    }

    @Test
    void getApplicationValidity_readsFromConfiguration() {
        assertThat(tradeLicenseService.getApplicationValidity()).isEqualTo(365);
    }

    @Test
    void enrichWithUserDetails_whenUserSearchEmpty_returnsTrue() {
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(userService.getUser(criteria, requestInfo)).thenReturn(new UserDetailResponse(null, Collections.emptyList()));

        assertThat(tradeLicenseService.enrichWithUserDetails(criteria, requestInfo)).isTrue();
    }

    @Test
    void validateMobileNumberUniqueness_whenSameTradeTypeOnMobile_throws() {
        OwnerInfo owner = OwnerInfo.builder().mobileNumber("9999999999").build();
        TradeUnit unit = TradeUnit.builder().tradeType("RETAIL.A").build();
        TradeLicenseDetail detail = TradeLicenseDetail.builder().owners(List.of(owner)).tradeUnits(List.of(unit)).build();
        TradeLicense license = TradeLicense.builder()
                .tenantId("pb.amritsar")
                .businessService(businessService_BPA)
                .applicationNumber("APP-NEW")
                .tradeLicenseDetail(detail)
                .build();
        TradeLicenseRequest request = TradeLicenseRequest.builder().requestInfo(requestInfo).licenses(List.of(license)).build();

        TradeLicense existing = TradeLicense.builder()
                .applicationNumber("APP-OLD")
                .status("APPROVED")
                .tradeLicenseDetail(TradeLicenseDetail.builder()
                        .tradeUnits(List.of(TradeUnit.builder().tradeType("RETAIL.B").build()))
                        .build())
                .build();

        when(userService.getUser(any(), eq(requestInfo)))
                .thenReturn(new UserDetailResponse(null, List.of(OwnerInfo.builder().uuid("u1").build())));
        when(repository.getLicenses(any(), any())).thenReturn(List.of(existing));
        when(enrichmentService.getTradeLicenseCriteriaFromIds(any())).thenReturn(TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build());
        when(enrichmentService.enrichTradeLicenseSearch(any(), any(), eq(requestInfo))).thenReturn(List.of(existing));

        assertThatThrownBy(() -> tradeLicenseService.validateMobileNumberUniqueness(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Same mobile number can not be used");
    }

    @Test
    void validateMobileNumberUniqueness_whenExistingRejected_ignoresConflict() {
        OwnerInfo owner = OwnerInfo.builder().mobileNumber("9999999999").build();
        TradeUnit unit = TradeUnit.builder().tradeType("RETAIL.A").build();
        TradeLicenseDetail detail = TradeLicenseDetail.builder().owners(List.of(owner)).tradeUnits(List.of(unit)).build();
        TradeLicense license = TradeLicense.builder()
                .tenantId("pb.amritsar")
                .businessService(businessService_BPA)
                .applicationNumber("APP-NEW")
                .tradeLicenseDetail(detail)
                .build();
        TradeLicenseRequest request = TradeLicenseRequest.builder().requestInfo(requestInfo).licenses(List.of(license)).build();

        TradeLicense existing = TradeLicense.builder()
                .applicationNumber("APP-OLD")
                .status(STATUS_REJECTED)
                .tradeLicenseDetail(TradeLicenseDetail.builder()
                        .tradeUnits(List.of(TradeUnit.builder().tradeType("RETAIL.B").build()))
                        .build())
                .build();

        when(userService.getUser(any(), eq(requestInfo)))
                .thenReturn(new UserDetailResponse(null, List.of(OwnerInfo.builder().uuid("u1").build())));
        when(repository.getLicenses(any(), any())).thenReturn(List.of(existing));
        when(enrichmentService.getTradeLicenseCriteriaFromIds(any())).thenReturn(TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build());
        when(enrichmentService.enrichTradeLicenseSearch(any(), any(), eq(requestInfo))).thenReturn(List.of(existing));

        tradeLicenseService.validateMobileNumberUniqueness(request);
    }

    @Test
    void checkEndStateAndAddBPARoles_whenStatusMatchesEndState_updatesUsersWithRoles() {
        TradeLicense license = TradeLicense.builder().status("APPROVED").tenantId("pb.amritsar").build();
        TradeLicenseRequest request = TradeLicenseRequest.builder().requestInfo(requestInfo).licenses(List.of(license)).build();
        when(tradeUtil.getBPAEndState(request)).thenReturn(List.of("APPROVED"));

        tradeLicenseService.checkEndStateAndAddBPARoles(request);

        verify(userService).createUser(any(TradeLicenseRequest.class), eq(true));
    }

    @Test
    void runJob_whenServiceNameNull_defaultsModuleAndDelegatesToBatch() {
        tradeLicenseService.runJob(null, "EXPIRE", requestInfo);
        verify(tlBatchService).getLicensesAndPerformAction(eq(TLConstants.TRADE_LICENSE_MODULE_CODE), eq("EXPIRE"), eq(requestInfo));
    }
}
