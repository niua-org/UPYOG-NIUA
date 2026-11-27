package org.egov.user.domain.service.family;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.asset.AssetAuthorizationService;
import org.egov.user.domain.service.config.FamilyConfigurationService;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FamilyRestRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyProfileServiceImplTest {

    @Mock
    private FamilyRestRepository familyRestRepository;

    @Mock
    private AssetAuthorizationService assetAuthorizationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserDocumentRepository userDocumentRepository;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Mock
    private FamilyConfigurationService familyConfigurationService;

    @Mock
    private UserConfiguration userConfiguration;

    private FamilyProfileServiceImpl familyProfileService;

    private RequestInfo requestInfo;
    private org.egov.common.contract.request.User callerUser;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(userConfiguration.getFamilyPrivilegedRoles())
                .thenReturn(java.util.Set.of("FAMILY_APPROVER", "FAMILY_VIEWER", "SUPERUSER", "SYSTEM", "INTERNAL_MICROSERVICE_ROLE"));
        org.mockito.Mockito.lenient().when(userConfiguration.getStateLevelTenantId()).thenReturn("pg");

        familyProfileService = new FamilyProfileServiceImpl(
                familyRestRepository,
                assetAuthorizationService,
                userRepository,
                addressRepository,
                userDocumentRepository,
                encryptionDecryptionUtil,
                familyConfigurationService,
                userConfiguration
        );

        callerUser = org.egov.common.contract.request.User.builder()
                .id(101L)
                .uuid("citizen-caller-uuid")
                .type("CITIZEN")
                .roles(Collections.singletonList(Role.builder().code("CITIZEN").build()))
                .build();

        requestInfo = RequestInfo.builder()
                .apiId("org.egov.user")
                .ver("1.0")
                .ts(1728470420000L)
                .action("search")
                .msgId("msg-001")
                .userInfo(callerUser)
                .build();
    }

    @Test
    @DisplayName("searchFamilyProfile: Citizen member successfully searches and receives full profile and assets")
    void testSearchFamilyProfile_Success_CitizenMember() {
        String familyId = "FAM-2026-001";
        String tenantId = "pg.citya";

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId(tenantId)
                .requestInfo(requestInfo)
                .build();

        FamilyMemberDetail headMember = FamilyMemberDetail.builder()
                .id("mem-head-1")
                .familyId(familyId)
                .userUuid("head-uuid-1")
                .relationshipWithHead("SELF")
                .isHead(true)
                .active(true)
                .status("ACTIVE")
                .build();

        FamilyMemberDetail member2 = FamilyMemberDetail.builder()
                .id("mem-2")
                .familyId(familyId)
                .userUuid("citizen-caller-uuid")
                .relationshipWithHead("SPOUSE")
                .isHead(false)
                .active(true)
                .status("ACTIVE")
                .build();

        List<FamilyMemberDetail> members = new ArrayList<>();
        members.add(headMember);
        members.add(member2);

        FamilyDetail familyDetail = FamilyDetail.builder()
                .id("fam-uuid-1")
                .familyId(familyId)
                .tenantId(tenantId)
                .headUserUuid("head-uuid-1")
                .headName("Rajesh Kumar")
                .status("ACTIVE")
                .category("GENERAL")
                .members(members)
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq(tenantId), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("citizen-caller-uuid");

        User headUser = User.builder()
                .id(1L)
                .uuid("head-uuid-1")
                .name("Rajesh Kumar")
                .mobileNumber("9876543210")
                .type(UserType.CITIZEN)
                .active(true)
                .gender(org.egov.user.domain.model.enums.Gender.MALE)
                .dob(new Date(631152000000L))
                .build();

        User callerDomainUser = User.builder()
                .id(2L)
                .uuid("citizen-caller-uuid")
                .name("Sunita Kumar")
                .mobileNumber("9876543211")
                .type(UserType.CITIZEN)
                .active(true)
                .gender(org.egov.user.domain.model.enums.Gender.FEMALE)
                .dob(new Date(662688000000L))
                .build();

        List<User> userList = List.of(headUser, callerDomainUser);
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(userList);
        when(encryptionDecryptionUtil.decryptObject(eq(headUser), eq(UserConstants.USER_ENCRYPTION_KEY), eq(User.class), eq(requestInfo)))
                .thenReturn(headUser);
        when(encryptionDecryptionUtil.decryptObject(eq(callerDomainUser), eq(UserConstants.USER_ENCRYPTION_KEY), eq(User.class), eq(requestInfo)))
                .thenReturn(callerDomainUser);

        // Mock Addresses
        Address addr = Address.builder()
                .id(10L)
                .userUuid("citizen-caller-uuid")
                .address("Flat 101, Galaxy Apts")
                .city("citya")
                .status("active")
                .build();
        when(addressRepository.getAddressV2(any(AddressSearchCriteria.class)))
                .thenReturn(Collections.singletonList(addr));

        // Mock Documents
        UserDocument doc = UserDocument.builder()
                .id("doc-1")
                .userUuid("citizen-caller-uuid")
                .documentType("AADHAAR")
                .filestoreId("file-uuid-1")
                .active(true)
                .build();
        when(userDocumentRepository.search(any(UserDocumentSearchCriteria.class)))
                .thenReturn(Collections.singletonList(doc));

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);

        assertNotNull(response);
        assertEquals(UserConstants.STATUS_SUCCESS, response.getResponseInfo().getStatus());
        FamilyDetail family = response.getFamily();
        assertNotNull(family);
        assertEquals(familyId, family.getFamilyId());
        assertEquals("Rajesh Kumar", family.getHeadName());
        assertEquals(2, family.getMembers().size());

        // Verify member 2 (caller)
        FamilyMemberDetail memberDetail2 = family.getMembers().get(1);
        assertEquals("citizen-caller-uuid", memberDetail2.getUserUuid());
        assertNotNull(memberDetail2.getProfile());
        assertEquals("Sunita Kumar", memberDetail2.getProfile().getName());
        assertNotNull(memberDetail2.getProfile().getAddresses());
        assertNotNull(memberDetail2.getProfile().getDocuments());

        // Verify member 1 (HoF / other member) receives summary profile only (data minimisation)
        FamilyMemberDetail memberDetail1 = family.getMembers().get(0);
        assertEquals("head-uuid-1", memberDetail1.getUserUuid());
        assertNotNull(memberDetail1.getProfile());
        assertEquals("Rajesh Kumar", memberDetail1.getProfile().getName());
        assertNull(memberDetail1.getProfile().getAddresses());
        assertNull(memberDetail1.getProfile().getDocuments());
    }

    @Test
    @DisplayName("searchFamilyProfile: Head of Family (HoF) caller receives full profile for all members")
    void testSearchFamilyProfile_HoFCaller_GetsFullViewForAllMembers() {
        String familyId = "FAM-2026-HOF";
        String tenantId = "pg.citya";

        org.egov.common.contract.request.User hofCaller = org.egov.common.contract.request.User.builder()
                .uuid("hof-uuid-1")
                .type("CITIZEN")
                .roles(Collections.singletonList(Role.builder().code("CITIZEN").build()))
                .build();
        RequestInfo hofRequestInfo = RequestInfo.builder()
                .userInfo(hofCaller)
                .build();

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId(tenantId)
                .requestInfo(hofRequestInfo)
                .build();

        FamilyMemberDetail member1 = FamilyMemberDetail.builder()
                .userUuid("hof-uuid-1")
                .isHead(true)
                .active(true)
                .status("ACTIVE")
                .build();
        FamilyMemberDetail member2 = FamilyMemberDetail.builder()
                .userUuid("member-uuid-2")
                .isHead(false)
                .active(true)
                .status("ACTIVE")
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId(tenantId)
                .headUserUuid("hof-uuid-1")
                .members(List.of(member1, member2))
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq(tenantId), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(hofRequestInfo)).thenReturn("hof-uuid-1");

        User user1 = User.builder().uuid("hof-uuid-1").name("HoF Name").active(true).build();
        User user2 = User.builder().uuid("member-uuid-2").name("Member Name").active(true).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(List.of(user1, user2));
        when(encryptionDecryptionUtil.decryptObject(eq(user1), any(), any(), any())).thenReturn(user1);
        when(encryptionDecryptionUtil.decryptObject(eq(user2), any(), any(), any())).thenReturn(user2);

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);

        assertNotNull(response);
        FamilyMemberDetail m2 = response.getFamily().getMembers().get(1);
        assertEquals("member-uuid-2", m2.getUserUuid());
        assertNotNull(m2.getProfile());
        assertEquals("Member Name", m2.getProfile().getName());
    }

    @Test
    @DisplayName("searchFamilyProfile: Privileged caller (EMPLOYEE with dedicated role) permitted to search cross-family with matching tenant")
    void testSearchFamilyProfile_Success_PrivilegedCaller() {
        String familyId = "FAM-2026-999";
        org.egov.common.contract.request.User employeeUser = org.egov.common.contract.request.User.builder()
                .uuid("emp-uuid-1")
                .type("EMPLOYEE")
                .tenantId("pg.citya")
                .roles(Collections.singletonList(Role.builder().code("FAMILY_APPROVER").build()))
                .build();
        RequestInfo empRequestInfo = RequestInfo.builder()
                .apiId("org.egov.user")
                .ver("1.0")
                .msgId("msg-002")
                .userInfo(employeeUser)
                .build();

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(empRequestInfo)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .headUserUuid("head-uuid-999")
                .members(Collections.emptyList())
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(empRequestInfo)).thenReturn("emp-uuid-1");

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);

        assertNotNull(response);
        assertEquals(UserConstants.STATUS_SUCCESS, response.getResponseInfo().getStatus());
        assertEquals(familyId, response.getFamily().getFamilyId());
    }

    @Test
    @DisplayName("searchFamilyProfile: Employee with generic EMPLOYEE role without dedicated role throws UNAUTHORIZED_ACCESS")
    void testSearchFamilyProfile_GenericEmployeeWithoutDedicatedRole_ThrowsUnauthorized() {
        String familyId = "FAM-2026-001";
        org.egov.common.contract.request.User genericEmp = org.egov.common.contract.request.User.builder()
                .uuid("generic-emp-uuid")
                .type("EMPLOYEE")
                .tenantId("pg.citya")
                .roles(Collections.singletonList(Role.builder().code("EMPLOYEE").build()))
                .build();
        RequestInfo empRequestInfo = RequestInfo.builder()
                .userInfo(genericEmp)
                .build();

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(empRequestInfo)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .headUserUuid("head-uuid-1")
                .members(Collections.emptyList())
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(empRequestInfo)).thenReturn("generic-emp-uuid");

        CustomException ex = assertThrows(CustomException.class, () -> familyProfileService.searchFamilyProfile(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("searchFamilyProfile: Employee with dedicated role but mismatched tenant throws UNAUTHORIZED_ACCESS")
    void testSearchFamilyProfile_EmployeeTenantMismatch_ThrowsUnauthorized() {
        String familyId = "FAM-2026-001";
        org.egov.common.contract.request.User otherUlbEmp = org.egov.common.contract.request.User.builder()
                .uuid("other-ulb-emp-uuid")
                .type("EMPLOYEE")
                .tenantId("pg.cityb") // Different ULB
                .roles(Collections.singletonList(Role.builder().code("FAMILY_APPROVER").build()))
                .build();
        RequestInfo empRequestInfo = RequestInfo.builder()
                .userInfo(otherUlbEmp)
                .build();

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(empRequestInfo)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId("pg.citya") // Family in citya
                .headUserUuid("head-uuid-1")
                .members(Collections.emptyList())
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(empRequestInfo)).thenReturn("other-ulb-emp-uuid");

        CustomException ex = assertThrows(CustomException.class, () -> familyProfileService.searchFamilyProfile(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("searchFamilyProfile: Inactive or non-ACTIVE citizen member throws UNAUTHORIZED_ACCESS")
    void testSearchFamilyProfile_InactiveMember_ThrowsUnauthorized() {
        String familyId = "FAM-2026-001";
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        FamilyMemberDetail inactiveMember = FamilyMemberDetail.builder()
                .id("mem-inactive")
                .familyId(familyId)
                .userUuid("citizen-caller-uuid")
                .relationshipWithHead("SON")
                .isHead(false)
                .active(false) // inactive member
                .status("REJECTED")
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .headUserUuid("head-uuid-1")
                .members(List.of(inactiveMember))
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("citizen-caller-uuid");

        CustomException ex = assertThrows(CustomException.class, () -> familyProfileService.searchFamilyProfile(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("searchFamilyProfile: Enriches headName if blank on family unit")
    void testSearchFamilyProfile_EnrichesHeadName() {
        String familyId = "FAM-2026-002";
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        FamilyMemberDetail headMember = FamilyMemberDetail.builder()
                .userUuid("head-uuid-1")
                .isHead(true)
                .active(true)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .headUserUuid("head-uuid-1")
                .headName(null) // headName is null
                .members(List.of(headMember))
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("head-uuid-1");

        User headUser = User.builder()
                .uuid("head-uuid-1")
                .name("Vikram Singh")
                .active(true)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(List.of(headUser));
        when(encryptionDecryptionUtil.decryptObject(eq(headUser), any(), any(), any())).thenReturn(headUser);

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);

        assertNotNull(response);
        assertEquals("Vikram Singh", response.getFamily().getHeadName());
    }

    @Test
    @DisplayName("searchFamilyProfile: Throws CustomException when family is not found")
    void testSearchFamilyProfile_FamilyNotFound() {
        String familyId = "FAM-NOT-FOUND";
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(null);

        CustomException exception = assertThrows(CustomException.class, () ->
                familyProfileService.searchFamilyProfile(request));

        assertEquals(UserConstants.ERROR_FAMILY_NOT_FOUND, exception.getCode());
    }

    @Test
    @DisplayName("searchFamilyProfile: Throws CustomException when citizen caller is neither HoF nor member")
    void testSearchFamilyProfile_UnauthorizedCitizen() {
        String familyId = "FAM-2026-001";
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .headUserUuid("other-head-uuid")
                .members(List.of(FamilyMemberDetail.builder().userUuid("other-member-uuid").build()))
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("intruder-uuid");

        CustomException exception = assertThrows(CustomException.class, () ->
                familyProfileService.searchFamilyProfile(request));

        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, exception.getCode());
    }

    @Test
    @DisplayName("searchFamilyProfile: Validations fail for null request, RequestInfo, or blank familyId")
    void testSearchFamilyProfile_ValidationFailures() {
        assertThrows(CustomException.class, () ->
                familyProfileService.searchFamilyProfile(null));

        FamilyProfileSearchRequest noInfo = FamilyProfileSearchRequest.builder()
                .familyId("FAM-1")
                .build();
        assertThrows(CustomException.class, () ->
                familyProfileService.searchFamilyProfile(noInfo));

        FamilyProfileSearchRequest blankId = FamilyProfileSearchRequest.builder()
                .familyId("   ")
                .requestInfo(requestInfo)
                .build();
        assertThrows(CustomException.class, () ->
                familyProfileService.searchFamilyProfile(blankId));
    }

    @Test
    @DisplayName("searchFamilyProfile: Member with no matching eg_user profile handled gracefully")
    void testSearchFamilyProfile_MemberNotYetRegisteredInEgUser() {
        String familyId = "FAM-2026-003";
        org.egov.common.contract.request.User employeeUser = org.egov.common.contract.request.User.builder()
                .uuid("emp-uuid-2")
                .type("EMPLOYEE")
                .tenantId("pg.citya")
                .roles(Collections.singletonList(Role.builder().code("FAMILY_APPROVER").build()))
                .build();
        RequestInfo empRequestInfo = RequestInfo.builder()
                .apiId("org.egov.user")
                .ver("1.0")
                .msgId("msg-003")
                .userInfo(employeeUser)
                .build();

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(empRequestInfo)
                .build();

        FamilyMemberDetail member = FamilyMemberDetail.builder()
                .userUuid("unregistered-uuid")
                .isHead(true)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .headUserUuid("unregistered-uuid")
                .members(List.of(member))
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(empRequestInfo)).thenReturn("emp-uuid-2");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);

        assertNotNull(response);
        FamilyMemberDetail m = response.getFamily().getMembers().get(0);
        assertNull(m.getProfile());
    }

    @Test
    @DisplayName("searchFamilyProfile: Falls back to configured state.level.tenant.id when request and family tenant are blank")
    void testSearchFamilyProfile_FallbackToConfiguredStateTenant() {
        String familyId = "FAM-2026-NO-TENANT";
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId(null) // Blank request tenant
                .requestInfo(requestInfo)
                .build();

        FamilyMemberDetail member = FamilyMemberDetail.builder()
                .userUuid("citizen-caller-uuid")
                .isHead(true)
                .active(true)
                .status("ACTIVE")
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId(null) // Blank family tenant
                .headUserUuid("citizen-caller-uuid")
                .members(List.of(member))
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), isNull(), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("citizen-caller-uuid");
        when(userConfiguration.getStateLevelTenantId()).thenReturn("pb");

        User user = User.builder().uuid("citizen-caller-uuid").name("Test User").active(true).build();
        org.mockito.ArgumentCaptor<UserSearchCriteria> criteriaCaptor = org.mockito.ArgumentCaptor.forClass(UserSearchCriteria.class);
        when(userRepository.findAll(criteriaCaptor.capture())).thenReturn(List.of(user));
        when(encryptionDecryptionUtil.decryptObject(eq(user), any(), any(), any())).thenReturn(user);

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);

        assertNotNull(response);
        assertEquals("pb", criteriaCaptor.getValue().getTenantId());
    }

    @Test
    @DisplayName("searchFamilyProfile: Throws TENANT_ID_REQUIRED when tenant is blank and state.level.tenant.id is not configured")
    void testSearchFamilyProfile_BlankTenantAndNoConfig_ThrowsTenantIdRequired() {
        String familyId = "FAM-2026-NO-TENANT-NO-CONFIG";
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId(null)
                .requestInfo(requestInfo)
                .build();

        FamilyMemberDetail member = FamilyMemberDetail.builder()
                .userUuid("citizen-caller-uuid")
                .isHead(true)
                .active(true)
                .status("ACTIVE")
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId(null)
                .headUserUuid("citizen-caller-uuid")
                .members(List.of(member))
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), isNull(), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("citizen-caller-uuid");
        when(userConfiguration.getStateLevelTenantId()).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class, () -> familyProfileService.searchFamilyProfile(request));
        assertEquals(UserConstants.ERROR_TENANT_ID_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("searchFamilyProfile: Denies employee access when no privileged roles are configured (empty set)")
    void testSearch_EmployeeAccessDenied_WhenNoPrivilegedRolesConfigured() {
        String familyId = "FAM-2026-EMPTY-ROLES";
        callerUser.setType("EMPLOYEE");
        callerUser.setRoles(Collections.singletonList(
                org.egov.common.contract.request.Role.builder().code("FAMILY_APPROVER").build()));
        callerUser.setTenantId("pg.citya");

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .headUserUuid("head-uuid-1")
                .members(Collections.emptyList())
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("emp-uuid");
        when(userConfiguration.getFamilyPrivilegedRoles()).thenReturn(Collections.emptySet());

        CustomException ex = assertThrows(CustomException.class, () -> familyProfileService.searchFamilyProfile(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("UserConfiguration: getFamilyPrivilegedRoles returns empty set when unconfigured without fallback")
    void testUserConfiguration_GetFamilyPrivilegedRoles_EmptyWhenUnconfigured() {
        UserConfiguration config = new UserConfiguration();
        assertNotNull(config.getFamilyPrivilegedRoles());
        assertTrue(config.getFamilyPrivilegedRoles().isEmpty());
    }

    @Test
    @DisplayName("searchFamilyProfile: Denies SYSTEM caller when caller lacks dedicated family access role")
    void testSearch_SystemCaller_WithoutConfiguredRole_Denied() {
        String familyId = "FAM-2026-SYS-DENIED";
        callerUser.setType("SYSTEM");
        callerUser.setRoles(Collections.singletonList(
                org.egov.common.contract.request.Role.builder().code("RANDOM_ROLE").build()));
        callerUser.setTenantId("pg.citya");

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .headUserUuid("head-uuid-1")
                .members(Collections.emptyList())
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("system-caller-uuid");

        CustomException ex = assertThrows(CustomException.class, () -> familyProfileService.searchFamilyProfile(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("searchFamilyProfile: Allows SYSTEM caller when caller has matching dedicated role")
    void testSearch_SystemCaller_WithConfiguredRole_Allowed() {
        String familyId = "FAM-2026-SYS-ALLOWED";
        callerUser.setType("SYSTEM");
        callerUser.setRoles(Collections.singletonList(
                org.egov.common.contract.request.Role.builder().code("INTERNAL_MICROSERVICE_ROLE").build()));
        callerUser.setTenantId("pg.citya");

        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .headUserUuid("head-uuid-1")
                .members(Collections.emptyList())
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.resolveCallerUuid(requestInfo)).thenReturn("system-caller-uuid");

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);
        assertNotNull(response);
        assertNotNull(response.getFamily());
    }
}
