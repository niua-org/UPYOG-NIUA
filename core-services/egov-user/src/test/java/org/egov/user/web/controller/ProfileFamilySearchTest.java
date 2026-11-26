package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.service.asset.MemberAssetService;
import org.egov.user.domain.service.family.FamilyProfileService;
import org.egov.user.domain.service.profile.ProfileService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.family.FamilyDetail;
import org.egov.user.web.contract.family.FamilyMemberDetail;
import org.egov.user.web.contract.family.FamilyProfileResponse;
import org.egov.user.web.contract.family.FamilyProfileSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test suite verifying family profile search endpoint in {@link ProfileController}.
 */
@ExtendWith(MockitoExtension.class)
class ProfileFamilySearchTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private MemberAssetService memberAssetService;

    @Mock
    private FamilyProfileService familyProfileService;

    private ProfileController profileController;

    @BeforeEach
    void setUp() {
        profileController = new ProfileController(profileService, memberAssetService, familyProfileService);
    }

    @Test
    @DisplayName("searchFamilyProfile: Successfully returns 200 OK with complete family details and member assets")
    void searchFamilyProfile_Success() {
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId("FAM-2026-001")
                .tenantId("pg.citya")
                .requestInfo(new RequestInfo())
                .build();

        FamilyProfileResponse expectedResponse = FamilyProfileResponse.builder()
                .responseInfo(ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build())
                .family(FamilyDetail.builder()
                        .familyId("FAM-2026-001")
                        .tenantId("pg.citya")
                        .headUserUuid("user-head-uuid")
                        .headName("Rajesh Kumar")
                        .members(Collections.singletonList(
                                FamilyMemberDetail.builder()
                                        .userUuid("user-head-uuid")
                                        .isHead(true)
                                        .build()
                        ))
                        .build())
                .build();

        when(familyProfileService.searchFamilyProfile(any(FamilyProfileSearchRequest.class)))
                .thenReturn(expectedResponse);

        ResponseEntity<FamilyProfileResponse> response = profileController.getFamilyProfileDetails(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FAM-2026-001", response.getBody().getFamily().getFamilyId());
        verify(familyProfileService).searchFamilyProfile(request);
    }

    @Test
    @DisplayName("getFamilyProfileDetails: Throws CustomException when familyProfileService is not wired")
    void getFamilyProfileDetails_ServiceUnavailable() {
        ProfileController controllerWithoutService = new ProfileController(profileService, memberAssetService);

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId("FAM-2026-001")
                .requestInfo(new RequestInfo())
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                controllerWithoutService.getFamilyProfileDetails(request));

        assertEquals(UserConstants.ERROR_SERVICE_UNAVAILABLE, ex.getCode());
    }

    @Test
    @DisplayName("getFamilyProfileDetails: Propagates CustomException when family is not found")
    void getFamilyProfileDetails_PropagatesNotFound() {
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId("FAM-NON-EXISTENT")
                .requestInfo(new RequestInfo())
                .build();

        when(familyProfileService.searchFamilyProfile(any(FamilyProfileSearchRequest.class)))
                .thenThrow(new CustomException(UserConstants.ERROR_FAMILY_NOT_FOUND, "Family not found"));

        CustomException ex = assertThrows(CustomException.class, () ->
                profileController.getFamilyProfileDetails(request));

        assertEquals(UserConstants.ERROR_FAMILY_NOT_FOUND, ex.getCode());
    }
}
