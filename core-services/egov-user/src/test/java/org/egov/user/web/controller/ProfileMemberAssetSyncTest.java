package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.service.asset.MemberAssetService;
import org.egov.user.domain.service.profile.ProfileService;
import org.egov.user.web.contract.asset.MemberAssetDetail;
import org.egov.user.web.contract.asset.MemberAssetSyncRequest;
import org.egov.user.web.contract.asset.MemberAssetSyncResponse;
import org.egov.user.web.contract.asset.MemberAssetSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test suite verifying member asset sync endpoint in {@link ProfileController}.
 */
@ExtendWith(MockitoExtension.class)
class ProfileMemberAssetSyncTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private MemberAssetService memberAssetService;

    private ProfileController profileController;

    @BeforeEach
    void setUp() {
        profileController = new ProfileController(profileService, memberAssetService);
    }

    @Test
    void syncMemberAssets_delegatesToServiceSuccessfully() {
        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(new RequestInfo())
                .members(Collections.singletonList(
                        MemberAssetDetail.builder()
                                .userUuid("citizen-uuid-1")
                                .tenantId("pg.citya")
                                .build()
                ))
                .build();

        MemberAssetSyncResponse expectedResponse = MemberAssetSyncResponse.builder()
                .responseInfo(ResponseInfo.builder().status("SUCCESS").build())
                .status("SUCCESS")
                .results(Collections.singletonList(
                        MemberAssetSyncResult.builder()
                                .userUuid("citizen-uuid-1")
                                .status("SUCCESS")
                                .build()
                ))
                .build();

        when(memberAssetService.syncMemberAssets(any(MemberAssetSyncRequest.class)))
                .thenReturn(expectedResponse);

        ResponseEntity<MemberAssetSyncResponse> responseEntity = profileController.syncMemberAssets(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("SUCCESS", responseEntity.getBody().getStatus());
        assertEquals(1, responseEntity.getBody().getResults().size());
        verify(memberAssetService, times(1)).syncMemberAssets(request);
    }
}
