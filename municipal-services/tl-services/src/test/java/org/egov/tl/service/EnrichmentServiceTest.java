package org.egov.tl.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.IdGenRepository;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.web.models.OwnerInfo;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseDetail;
import org.egov.tl.web.models.TradeLicenseSearchCriteria;
import org.egov.tl.web.models.user.UserDetailResponse;
import org.egov.tl.workflow.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.egov.tl.util.TLConstants.businessService_TL;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {

    @Mock private IdGenRepository idGenRepository;
    @Mock private TLConfiguration config;
    @Mock private TradeUtil tradeUtil;
    @Mock private BoundaryService boundaryService;
    @Mock private UserService userService;
    @Mock private WorkflowService workflowService;

    @InjectMocks
    private EnrichmentService enrichmentService;

    @Test
    void enrichSearchCriteriaWithAccountId_whenCitizenTenantOnly_setsAccountAndMobile() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("CITIZEN").uuid("citizen-uuid").userName("9999999999").build())
                .build();
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();

        enrichmentService.enrichSearchCriteriaWithAccountId(requestInfo, criteria);

        assertThat(criteria.getAccountId()).isEqualTo("citizen-uuid");
        assertThat(criteria.getMobileNumber()).isEqualTo("9999999999");
    }

    @Test
    void getTradeLicenseCriteriaFromIds_collectsLicenseIds() {
        TradeLicense first = TradeLicense.builder().id("id-1").businessService(businessService_TL).build();
        TradeLicense second = TradeLicense.builder().id("id-2").businessService(businessService_TL).build();

        TradeLicenseSearchCriteria criteria = enrichmentService.getTradeLicenseCriteriaFromIds(List.of(first, second));

        assertThat(criteria.getIds()).containsExactlyInAnyOrder("id-1", "id-2");
        assertThat(criteria.getBusinessService()).isEqualTo(businessService_TL);
    }

    @Test
    void enrichTLCriteriaWithOwnerids_whenMissing_populatesFromUserSearch() {
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        UserDetailResponse response = new UserDetailResponse(null, List.of(OwnerInfo.builder().uuid("owner-1").build()));

        enrichmentService.enrichTLCriteriaWithOwnerids(criteria, response);

        assertThat(criteria.getOwnerIds()).containsExactly("owner-1");
    }

    @Test
    void enrichTLSearchCriteriaWithOwnerids_collectsOwnerUuidsFromLicenses() {
        OwnerInfo owner = OwnerInfo.builder().uuid("owner-uuid").build();
        TradeLicenseDetail detail = TradeLicenseDetail.builder().owners(List.of(owner)).build();
        TradeLicense license = TradeLicense.builder().businessService(businessService_TL).tradeLicenseDetail(detail).build();
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();

        TradeLicenseSearchCriteria enriched = enrichmentService.enrichTLSearchCriteriaWithOwnerids(criteria, List.of(license));

        assertThat(enriched.getOwnerIds()).containsExactly("owner-uuid");
        assertThat(enriched.getTenantId()).isEqualTo("pb.amritsar");
    }
}
