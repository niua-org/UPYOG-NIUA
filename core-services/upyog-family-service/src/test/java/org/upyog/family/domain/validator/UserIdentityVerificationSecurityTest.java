package org.upyog.family.domain.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.FileStoreClient;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.persistence.repository.UserClientImpl;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.*;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserIdentityVerificationSecurityTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FamilyConfiguration configuration;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FileStoreClient fileStoreClient;

    @Mock
    private FamilyMasterDataService familyMasterDataService;

    @Mock
    private RequesterDxClient requesterDxClient;

    private UserClientImpl userClient;
    private FamilyValidator familyValidator;
    private AddMemberValidator addMemberValidator;
    private JoinFamilyValidator joinFamilyValidator;

    @BeforeEach
    void setUp() {
        when(configuration.getUserHost()).thenReturn("http://localhost:8081");
        when(configuration.getUserSearchPath()).thenReturn("/user/_search");

        userClient = new UserClientImpl(restTemplate, configuration);
        familyValidator = new FamilyValidator(familyRepository, fileStoreClient, familyMasterDataService, userClient);
        addMemberValidator = new AddMemberValidator(familyRepository, familyMasterDataService, requesterDxClient, userClient);
        joinFamilyValidator = new JoinFamilyValidator(familyRepository, familyMasterDataService, requesterDxClient, userClient);
    }

    private RequestInfo createCitizenRequestInfo(String uuid) {
        return RequestInfo.builder()
                .userInfo(User.builder()
                        .uuid(uuid)
                        .type("CITIZEN")
                        .build())
                .build();
    }

    private void mockUserServiceResponse(String uuid, Boolean isIdentityVerified) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uuid", uuid);
        userMap.put("userName", "9876543210");
        userMap.put("isIdentityVerified", isIdentityVerified);

        Map<String, Object> response = new HashMap<>();
        response.put("user", Collections.singletonList(userMap));

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);
    }

    @Test
    @DisplayName("UserClient: Should reject unverified citizen caller with USER_NOT_VERIFIED")
    void testValidateCallerIsVerified_UnverifiedCitizen_ThrowsException() {
        RequestInfo requestInfo = createCitizenRequestInfo("citizen-unverified-uuid");
        mockUserServiceResponse("citizen-unverified-uuid", false);

        CustomException ex = assertThrows(CustomException.class, () ->
                userClient.validateCallerIsVerified(requestInfo)
        );

        assertEquals(FamilyConstants.ERROR_USER_NOT_VERIFIED, ex.getCode());
        assertTrue(ex.getMessage().contains("User identity is not verified"));
    }

    @Test
    @DisplayName("UserClient: Should reject citizen caller with null isIdentityVerified flag")
    void testValidateCallerIsVerified_NullVerifiedFlag_ThrowsException() {
        RequestInfo requestInfo = createCitizenRequestInfo("citizen-null-flag-uuid");
        mockUserServiceResponse("citizen-null-flag-uuid", null);

        CustomException ex = assertThrows(CustomException.class, () ->
                userClient.validateCallerIsVerified(requestInfo)
        );

        assertEquals(FamilyConstants.ERROR_USER_NOT_VERIFIED, ex.getCode());
    }

    @Test
    @DisplayName("UserClient: Should reject citizen caller not found in user database")
    void testValidateCallerIsVerified_UserNotFound_ThrowsException() {
        RequestInfo requestInfo = createCitizenRequestInfo("citizen-not-found-uuid");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Collections.emptyMap());

        CustomException ex = assertThrows(CustomException.class, () ->
                userClient.validateCallerIsVerified(requestInfo)
        );

        assertEquals(FamilyConstants.ERROR_USER_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("UserClient: Should succeed when citizen caller has isIdentityVerified=true in DB")
    void testValidateCallerIsVerified_VerifiedCitizen_Success() {
        RequestInfo requestInfo = createCitizenRequestInfo("citizen-verified-uuid");
        mockUserServiceResponse("citizen-verified-uuid", true);

        CitizenProfile profile = assertDoesNotThrow(() ->
                userClient.validateCallerIsVerified(requestInfo)
        );

        assertNotNull(profile);
        assertTrue(profile.getIsIdentityVerified());
    }

    @Test
    @DisplayName("UserClient: Should bypass verification for SYSTEM_USER and EMPLOYEE callers")
    void testValidateCallerIsVerified_SystemAndEmployee_BypassesCheck() {
        RequestInfo systemRequest = RequestInfo.builder()
                .userInfo(User.builder().uuid(FamilyConstants.SYSTEM_USER).type("SYSTEM").build())
                .build();
        assertNull(userClient.validateCallerIsVerified(systemRequest));

        RequestInfo employeeRequest = RequestInfo.builder()
                .userInfo(User.builder().uuid("emp-uuid").type("EMPLOYEE").build())
                .build();
        assertNull(userClient.validateCallerIsVerified(employeeRequest));
    }

    @Test
    @DisplayName("FamilyValidator: createFamily should reject when caller is unverified in DB")
    void testFamilyValidator_CreateFamily_UnverifiedCaller_ThrowsException() {
        String callerUuid = "unverified-creator-uuid";
        RequestInfo requestInfo = createCitizenRequestInfo(callerUuid);
        mockUserServiceResponse(callerUuid, false);

        FamilyMember head = FamilyMember.builder()
                .userUuid(callerUuid)
                .relationshipWithHead("HEAD")
                .isHead(true)
                .build();

        Family family = Family.builder()
                .tenantId("pg.citya")
                .headUserUuid(callerUuid)
                .members(Collections.singletonList(head))
                .build();

        FamilyRequest request = new FamilyRequest(requestInfo, family);

        CustomException ex = assertThrows(CustomException.class, () ->
                familyValidator.validateCreate(request)
        );

        assertEquals(FamilyConstants.ERROR_USER_NOT_VERIFIED, ex.getCode());
    }

    @Test
    @DisplayName("AddMemberValidator: addMember should reject when HoF caller is unverified in DB")
    void testAddMemberValidator_UnverifiedCaller_ThrowsException() {
        String hofUuid = "unverified-hof-uuid";
        RequestInfo requestInfo = createCitizenRequestInfo(hofUuid);
        mockUserServiceResponse(hofUuid, false);

        Family activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid(hofUuid)
                .build();

        AddMemberRequest request = AddMemberRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .member(FamilyMember.builder()
                        .userUuid("new-member-uuid")
                        .relationshipWithHead("SON")
                        .build())
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                addMemberValidator.validate(request, activeFamily)
        );

        assertEquals(FamilyConstants.ERROR_USER_NOT_VERIFIED, ex.getCode());
    }

    @Test
    @DisplayName("JoinFamilyValidator: joinFamily should reject when applicant is unverified in DB")
    void testJoinFamilyValidator_UnverifiedApplicant_ThrowsException() {
        String applicantUuid = "unverified-applicant-uuid";
        RequestInfo requestInfo = createCitizenRequestInfo(applicantUuid);
        mockUserServiceResponse(applicantUuid, false);

        Family activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("hof-uuid")
                .build();

        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(requestInfo)
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid(applicantUuid)
                .relationshipWithHead("SON")
                .hofConsentOtp("CONSENT-VERIFIED-12345")
                .build();

        CustomException ex = assertThrows(CustomException.class, () ->
                joinFamilyValidator.validate(request, activeFamily)
        );

        assertEquals(FamilyConstants.ERROR_USER_NOT_VERIFIED, ex.getCode());
    }
}
