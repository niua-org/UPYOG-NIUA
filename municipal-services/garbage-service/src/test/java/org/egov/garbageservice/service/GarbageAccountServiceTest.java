package org.egov.garbageservice.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.repository.GarbageAccountRepository;
import org.egov.garbageservice.util.GrbgConstants;
import org.egov.garbageservice.util.ResponseInfoFactory;
import org.egov.garbageservice.web.models.GarbageAccountResponse;
import org.egov.garbageservice.web.models.SearchCriteriaGarbageAccount;
import org.egov.garbageservice.web.models.SearchCriteriaGarbageAccountRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GarbageAccountServiceTest {

    @Mock private GarbageAccountRepository garbageAccountRepository;
    @Mock private ResponseInfoFactory responseInfoFactory;

    @InjectMocks
    private GarbageAccountService garbageAccountService;

    @Test
    void getStatusOrAction_whenApproveAction_returnsApprovedStatus() {
        assertThat(garbageAccountService.getStatusOrAction(GrbgConstants.WORKFLOW_ACTION_APPROVE, true))
                .isEqualTo(GrbgConstants.STATUS_APPROVED);
    }

    @Test
    void getStatusOrAction_whenApprovedStatus_returnsApproveAction() {
        assertThat(garbageAccountService.getStatusOrAction(GrbgConstants.STATUS_APPROVED, false))
                .isEqualTo(GrbgConstants.WORKFLOW_ACTION_APPROVE);
    }

    @Test
    void searchGarbageAccounts_whenRepositoryEmpty_returnsEmptyResponse() {
        SearchCriteriaGarbageAccountRequest request = SearchCriteriaGarbageAccountRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .searchCriteriaGarbageAccount(SearchCriteriaGarbageAccount.builder()
                        .tenantId("pb.amritsar")
                        .isSchedulerCall(true)
                        .build())
                .isSchedulerCall(true)
                .build();
        when(garbageAccountRepository.searchGarbageAccount(any(), any())).thenReturn(Collections.emptyList());
        when(responseInfoFactory.createResponseInfoFromRequestInfo(any(), any(Boolean.class)))
                .thenReturn(new org.egov.common.contract.response.ResponseInfo());

        GarbageAccountResponse response = garbageAccountService.searchGarbageAccounts(request);

        assertThat(response.getGarbageAccounts()).isNullOrEmpty();
        assertThat(response.getApplicationCount()).isZero();
    }
}
