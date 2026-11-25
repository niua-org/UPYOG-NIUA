package org.egov.user.domain.service.family;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.asset.AssetAuthorizationService;
import org.egov.user.domain.service.config.FamilyConfigurationService;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FamilyRestRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserIncomeRepository;
import org.egov.user.persistence.repository.UserPropertyRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.persistence.repository.UserVehicleRepository;
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
    private UserPropertyRepository userPropertyRepository;

    @Mock
    private UserIncomeRepository userIncomeRepository;

    @Mock
    private UserVehicleRepository userVehicleRepository;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Mock
    private FamilyConfigurationService familyConfigurationService;

    private FamilyProfileServiceImpl familyProfileService;

    private RequestInfo requestInfo;
    private org.egov.common.contract.request.User callerUser;

    @BeforeEach
    void setUp() {
        familyProfileService = new FamilyProfileServiceImpl(
                familyRestRepository,
                assetAuthorizationService,
                userRepository,
                addressRepository,
                userDocumentRepository,
                userPropertyRepository,
                userIncomeRepository,
                userVehicleRepository,
                encryptionDecryptionUtil,
                familyConfigurationService
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
        when(assetAuthorizationService.isPrivilegedCaller(requestInfo)).thenReturn(false);
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

        // Mock Assets: Vehicles
        UserVehicle vehicle = UserVehicle.builder()
                .id("veh-1")
                .userUuid("citizen-caller-uuid")
                .vehicleNumber("HR26DK8899")
                .vehicleType("FOUR_WHEELER")
                .status("ACTIVE")
                .build();
        when(userVehicleRepository.findVehiclesByUserUuids(anyList(), eq(Boolean.TRUE)))
                .thenReturn(Map.of("citizen-caller-uuid", List.of(vehicle)));

        // Mock Assets: Income
        UserIncome income = UserIncome.builder()
                .id("inc-1")
                .userUuid("citizen-caller-uuid")
                .annualIncome(java.math.BigDecimal.valueOf(650000.0))
                .occupation("SALARIED")
                .status("ACTIVE")
                .build();
        when(userIncomeRepository.findActiveIncomesByUserUuids(anyList(), eq(Boolean.TRUE)))
                .thenReturn(Map.of("citizen-caller-uuid", income));

        // Mock Assets: Properties
        UserProperty property = UserProperty.builder()
                .id("prop-1")
                .userUuid("citizen-caller-uuid")
                .localityType("URBAN")
                .status("ACTIVE")
                .build();
        when(userPropertyRepository.findPropertiesByUserUuids(anyList(), eq(Boolean.TRUE)))
                .thenReturn(Map.of("citizen-caller-uuid", List.of(property)));

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
        assertEquals(1, memberDetail2.getVehicles().size());
        assertEquals("HR26DK8899", memberDetail2.getVehicles().get(0).getVehicleNumber());
        assertNotNull(memberDetail2.getIncome());
        assertEquals(java.math.BigDecimal.valueOf(650000.0), memberDetail2.getIncome().getAnnualIncome());
        assertEquals(1, memberDetail2.getProperties().size());
        assertEquals("URBAN", memberDetail2.getProperties().get(0).getLocalityType());

        // Verify profile also holds assets
        assertNotNull(memberDetail2.getProfile().getVehicles());
        assertEquals(1, memberDetail2.getProfile().getVehicles().size());
        assertNotNull(memberDetail2.getProfile().getIncome());
        assertNotNull(memberDetail2.getProfile().getProperties());
        assertEquals(1, memberDetail2.getProfile().getProperties().size());
        assertNotNull(memberDetail2.getProfile().getAddresses());
        assertNotNull(memberDetail2.getProfile().getDocuments());
    }

    @Test
    @DisplayName("searchFamilyProfile: Privileged caller (EMPLOYEE) permitted to search cross-family")
    void testSearchFamilyProfile_Success_PrivilegedCaller() {
        String familyId = "FAM-2026-999";
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .headUserUuid("head-uuid-999")
                .members(Collections.emptyList())
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.isPrivilegedCaller(requestInfo)).thenReturn(true);

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);

        assertNotNull(response);
        assertEquals(UserConstants.STATUS_SUCCESS, response.getResponseInfo().getStatus());
        assertEquals(familyId, response.getFamily().getFamilyId());
        verify(assetAuthorizationService, never()).resolveCallerUuid(any());
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
        when(assetAuthorizationService.isPrivilegedCaller(requestInfo)).thenReturn(false);
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
        when(assetAuthorizationService.isPrivilegedCaller(requestInfo)).thenReturn(false);
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
        FamilyProfileSearchRequest request = FamilyProfileSearchRequest.builder()
                .familyId(familyId)
                .tenantId("pg.citya")
                .requestInfo(requestInfo)
                .build();

        FamilyMemberDetail member = FamilyMemberDetail.builder()
                .userUuid("unregistered-uuid")
                .isHead(true)
                .build();

        FamilyDetail familyDetail = FamilyDetail.builder()
                .familyId(familyId)
                .headUserUuid("unregistered-uuid")
                .members(List.of(member))
                .build();

        when(familyRestRepository.getFamilyByFamilyId(eq(familyId), eq("pg.citya"), any(RequestInfo.class)))
                .thenReturn(familyDetail);
        when(assetAuthorizationService.isPrivilegedCaller(requestInfo)).thenReturn(true);
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        FamilyProfileResponse response = familyProfileService.searchFamilyProfile(request);

        assertNotNull(response);
        FamilyMemberDetail m = response.getFamily().getMembers().get(0);
        assertNull(m.getProfile());
        assertTrue(m.getVehicles().isEmpty());
        assertNull(m.getIncome());
        assertTrue(m.getProperties().isEmpty());
    }
}
