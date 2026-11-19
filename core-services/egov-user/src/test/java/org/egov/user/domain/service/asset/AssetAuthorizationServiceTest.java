package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.FamilyRestRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying security boundaries, Head of Family delegation, and caller authorization in {@link AssetAuthorizationService}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class AssetAuthorizationServiceTest {

    private UserRepository userRepository;
    private FamilyRestRepository familyRestRepository;
    private UserConfiguration configuration;
    private AssetAuthorizationService assetAuthorizationService;

    private static final String CITIZEN_CALLER_UUID = "citizen-uuid-001";
    private static final String MEMBER_UUID = "family-member-uuid-002";
    private static final String STRANGER_UUID = "stranger-citizen-uuid-003";
    private static final String EMPLOYEE_CALLER_UUID = "employee-uuid-999";
    private static final String TENANT_ID = "pg.citya";

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        familyRestRepository = mock(FamilyRestRepository.class);
        configuration = mock(UserConfiguration.class);
        when(configuration.getAssetPrivilegedRoles()).thenReturn(Set.of("EMPLOYEE", "SYSTEM", "INTERNAL_MICROSERVICE_ROLE", "SUPERUSER", "ADMIN", "CSC_OPERATOR"));
        assetAuthorizationService = new AssetAuthorizationService(userRepository, familyRestRepository, configuration);
    }

    private RequestInfo createCitizenRequestInfo(String uuid) {
        User userInfo = User.builder()
                .uuid(uuid)
                .id(101L)
                .type("CITIZEN")
                .roles(Collections.singletonList(Role.builder().code("CITIZEN").name("Citizen").build()))
                .build();
        return RequestInfo.builder().userInfo(userInfo).build();
    }

    private RequestInfo createEmployeeRequestInfo(String uuid) {
        User userInfo = User.builder()
                .uuid(uuid)
                .id(999L)
                .type("EMPLOYEE")
                .roles(Collections.singletonList(Role.builder().code("EMPLOYEE").name("Employee").build()))
                .build();
        return RequestInfo.builder().userInfo(userInfo).build();
    }

    @Test
    @DisplayName("Should successfully resolve caller UUID from valid RequestInfo")
    public void testResolveCallerUuid_Success() {
        RequestInfo requestInfo = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        String callerUuid = assetAuthorizationService.resolveCallerUuid(requestInfo);
        assertEquals(CITIZEN_CALLER_UUID, callerUuid);
    }

    @Test
    @DisplayName("Should throw CALLER_INFO_REQUIRED when RequestInfo or userInfo is null or blank")
    public void testResolveCallerUuid_ThrowsException_WhenUserInfoMissing() {
        CustomException ex1 = assertThrows(CustomException.class, () -> assetAuthorizationService.resolveCallerUuid(null));
        assertEquals(UserConstants.ERROR_CALLER_INFO_REQUIRED, ex1.getCode());

        RequestInfo reqWithoutUser = RequestInfo.builder().build();
        CustomException ex2 = assertThrows(CustomException.class, () -> assetAuthorizationService.resolveCallerUuid(reqWithoutUser));
        assertEquals(UserConstants.ERROR_CALLER_INFO_REQUIRED, ex2.getCode());

        RequestInfo reqWithBlankUuid = RequestInfo.builder()
                .userInfo(User.builder().uuid("   ").build())
                .build();
        CustomException ex3 = assertThrows(CustomException.class, () -> assetAuthorizationService.resolveCallerUuid(reqWithBlankUuid));
        assertEquals(UserConstants.ERROR_CALLER_INFO_REQUIRED, ex3.getCode());
    }

    @Test
    @DisplayName("Should correctly identify privileged callers by user type or role")
    public void testIsPrivilegedCaller() {
        assertTrue(assetAuthorizationService.isPrivilegedCaller(createEmployeeRequestInfo(EMPLOYEE_CALLER_UUID)));

        RequestInfo systemCaller = RequestInfo.builder()
                .userInfo(User.builder().uuid("sys-1").type("SYSTEM").build())
                .build();
        assertTrue(assetAuthorizationService.isPrivilegedCaller(systemCaller));

        RequestInfo adminRoleCaller = RequestInfo.builder()
                .userInfo(User.builder().uuid("admin-1").type("CITIZEN")
                        .roles(Collections.singletonList(Role.builder().code("ADMIN").name("Admin").build()))
                        .build())
                .build();
        assertTrue(assetAuthorizationService.isPrivilegedCaller(adminRoleCaller));

        assertFalse(assetAuthorizationService.isPrivilegedCaller(createCitizenRequestInfo(CITIZEN_CALLER_UUID)));
        assertFalse(assetAuthorizationService.isPrivilegedCaller(null));
    }

    @Test
    @DisplayName("Should permit asset access when caller is self (citizen accessing own asset)")
    public void testValidateAssetAccess_SelfAccess_Allowed() {
        RequestInfo citizenReq = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        // Does not throw exception
        assetAuthorizationService.validateAssetAccess(CITIZEN_CALLER_UUID, CITIZEN_CALLER_UUID, TENANT_ID, citizenReq);
    }

    @Test
    @DisplayName("Should permit asset access when caller is privileged employee")
    public void testValidateAssetAccess_PrivilegedCaller_Allowed() {
        RequestInfo employeeReq = createEmployeeRequestInfo(EMPLOYEE_CALLER_UUID);
        // Does not throw exception even though target is different citizen
        assetAuthorizationService.validateAssetAccess(EMPLOYEE_CALLER_UUID, CITIZEN_CALLER_UUID, TENANT_ID, employeeReq);
    }

    @Test
    @DisplayName("Should permit asset access when citizen caller is verified Head of Family for target member")
    public void testValidateAssetAccess_HeadOfFamilyDelegation_Allowed() {
        RequestInfo citizenReq = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(CITIZEN_CALLER_UUID), eq(MEMBER_UUID), eq(TENANT_ID), any()))
                .thenReturn(true);

        // Does not throw exception
        assetAuthorizationService.validateAssetAccess(CITIZEN_CALLER_UUID, MEMBER_UUID, TENANT_ID, citizenReq);
    }

    @Test
    @DisplayName("Should reject asset access with UNAUTHORIZED_ACCESS when citizen caller is NOT Head of Family")
    public void testValidateAssetAccess_StrangerAccess_ThrowsUnauthorized() {
        RequestInfo citizenReq = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(CITIZEN_CALLER_UUID), eq(STRANGER_UUID), eq(TENANT_ID), any()))
                .thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> assetAuthorizationService.validateAssetAccess(CITIZEN_CALLER_UUID, STRANGER_UUID, TENANT_ID, citizenReq));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("Should auto-scope search criteria to caller UUID for unconstrained citizen search")
    public void testScopeSearchCriteria_AutoScopesToCaller() {
        RequestInfo citizenReq = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        AtomicReference<String> scopedUuid = new AtomicReference<>();

        assetAuthorizationService.scopeSearchCriteria(
                CITIZEN_CALLER_UUID,
                null,
                null,
                TENANT_ID,
                citizenReq,
                scopedUuid::set
        );

        assertEquals(CITIZEN_CALLER_UUID, scopedUuid.get());
    }

    @Test
    @DisplayName("Should permit scoped search when citizen caller searches for own records")
    public void testScopeSearchCriteria_SelfSearch_Allowed() {
        RequestInfo citizenReq = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        AtomicReference<String> scopedUuid = new AtomicReference<>();

        assetAuthorizationService.scopeSearchCriteria(
                CITIZEN_CALLER_UUID,
                CITIZEN_CALLER_UUID,
                null,
                TENANT_ID,
                citizenReq,
                scopedUuid::set
        );

        // Not overridden, no exception
    }

    @Test
    @DisplayName("Should permit scoped search when citizen caller is Head of Family for member")
    public void testScopeSearchCriteria_HeadOfFamilySearch_Allowed() {
        RequestInfo citizenReq = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(CITIZEN_CALLER_UUID), eq(MEMBER_UUID), eq(TENANT_ID), any()))
                .thenReturn(true);

        assetAuthorizationService.scopeSearchCriteria(
                CITIZEN_CALLER_UUID,
                MEMBER_UUID,
                null,
                TENANT_ID,
                citizenReq,
                null
        );
    }

    @Test
    @DisplayName("Should reject scoped search when citizen caller searches another citizen without HoF")
    public void testScopeSearchCriteria_UnauthorizedCitizenSearch_ThrowsException() {
        RequestInfo citizenReq = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(CITIZEN_CALLER_UUID), eq(STRANGER_UUID), eq(TENANT_ID), any()))
                .thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> assetAuthorizationService.scopeSearchCriteria(
                        CITIZEN_CALLER_UUID,
                        STRANGER_UUID,
                        null,
                        TENANT_ID,
                        citizenReq,
                        null
                ));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("Should permit employee to search without auto-scoping or authorization check")
    public void testScopeSearchCriteria_PrivilegedEmployee_NoAutoScoping() {
        RequestInfo employeeReq = createEmployeeRequestInfo(EMPLOYEE_CALLER_UUID);
        AtomicReference<String> scopedUuid = new AtomicReference<>();

        assetAuthorizationService.scopeSearchCriteria(
                EMPLOYEE_CALLER_UUID,
                null,
                null,
                TENANT_ID,
                employeeReq,
                scopedUuid::set
        );

        // Employee search is unconstrained
        assertFalse(scopedUuid.get() != null && scopedUuid.get().equals(EMPLOYEE_CALLER_UUID));
    }

    @Test
    @DisplayName("Should permit caller with dynamically configured privileged role from application.properties")
    public void testIsPrivilegedCaller_ConfiguredRole_ReturnsTrue() {
        User userInfo = User.builder()
                .uuid("admin-uuid-001")
                .type("CITIZEN")
                .roles(Collections.singletonList(Role.builder().code("CSC_OPERATOR").name("CSC Operator").build()))
                .build();
        RequestInfo req = RequestInfo.builder().userInfo(userInfo).build();

        assertTrue(assetAuthorizationService.isPrivilegedCaller(req));
    }

    @Test
    @DisplayName("Should reject caller when role is not in configured privileged roles")
    public void testIsPrivilegedCaller_UnconfiguredRole_ReturnsFalse() {
        User userInfo = User.builder()
                .uuid("citizen-uuid-001")
                .type("CITIZEN")
                .roles(Collections.singletonList(Role.builder().code("CUSTOM_CITIZEN_ROLE").name("Custom").build()))
                .build();
        RequestInfo req = RequestInfo.builder().userInfo(userInfo).build();

        assertFalse(assetAuthorizationService.isPrivilegedCaller(req));
    }

    @Test
    @DisplayName("Should authorize viewing assets for self, HOF, and employee, and reject for strangers and unauthenticated")
    public void testIsAuthorizedToViewAssets_Scenarios() {
        RequestInfo citizenReq = createCitizenRequestInfo(CITIZEN_CALLER_UUID);
        RequestInfo employeeReq = createEmployeeRequestInfo(EMPLOYEE_CALLER_UUID);

        // 1. Self: Citizen viewing their own assets -> true
        assertTrue(assetAuthorizationService.isAuthorizedToViewAssets(CITIZEN_CALLER_UUID, TENANT_ID, citizenReq));

        // 2. Privileged Employee: Employee viewing any citizen's assets -> true
        assertTrue(assetAuthorizationService.isAuthorizedToViewAssets(STRANGER_UUID, TENANT_ID, employeeReq));

        // 3. Head of Family (HoF): HoF viewing family member's assets -> true
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(CITIZEN_CALLER_UUID), eq(MEMBER_UUID), eq(TENANT_ID), any()))
                .thenReturn(true);
        assertTrue(assetAuthorizationService.isAuthorizedToViewAssets(MEMBER_UUID, TENANT_ID, citizenReq));

        // 4. Stranger: Non-HOF citizen viewing stranger's assets -> false
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(CITIZEN_CALLER_UUID), eq(STRANGER_UUID), eq(TENANT_ID), any()))
                .thenReturn(false);
        assertFalse(assetAuthorizationService.isAuthorizedToViewAssets(STRANGER_UUID, TENANT_ID, citizenReq));

        // 5. Unauthenticated: null RequestInfo or missing userInfo -> false
        assertFalse(assetAuthorizationService.isAuthorizedToViewAssets(CITIZEN_CALLER_UUID, TENANT_ID, null));
        assertFalse(assetAuthorizationService.isAuthorizedToViewAssets(CITIZEN_CALLER_UUID, TENANT_ID, RequestInfo.builder().build()));

        // 6. Blank target UUID -> false
        assertFalse(assetAuthorizationService.isAuthorizedToViewAssets("", TENANT_ID, citizenReq));
        assertFalse(assetAuthorizationService.isAuthorizedToViewAssets(null, TENANT_ID, citizenReq));
    }
}
