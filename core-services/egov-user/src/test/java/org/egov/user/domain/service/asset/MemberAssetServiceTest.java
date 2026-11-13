package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.PropertyOwner;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UrbanPropertyDetail;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserIncomeRepository;
import org.egov.user.persistence.repository.UserPropertyRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.persistence.repository.UserVehicleRepository;
import org.egov.user.web.contract.asset.CitizenProfileDetail;
import org.egov.user.web.contract.asset.MemberAssetDetail;
import org.egov.user.web.contract.asset.MemberAssetSyncRequest;
import org.egov.user.web.contract.asset.MemberAssetSyncResponse;
import org.egov.user.web.contract.asset.MemberAssetSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test suite for {@link MemberAssetServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class MemberAssetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPropertyRepository propertyRepository;

    @Mock
    private UserIncomeRepository incomeRepository;

    @Mock
    private UserVehicleRepository vehicleRepository;

    @Mock
    private UserDocumentRepository documentRepository;

    private MemberAssetServiceImpl memberAssetService;

    @BeforeEach
    void setUp() {
        // Fix for PR Comment #4: Explicitly configure system actor credentials without inline fallback defaults
        UserConfiguration userConfig = UserConfiguration.builder()
                .systemActorId(1L)
                .systemActorUuid("SYSTEM")
                .requireCallerForSync(false)
                .build();
        MemberAssetProcessor processor = new MemberAssetProcessor(
                userRepository,
                propertyRepository,
                incomeRepository,
                vehicleRepository,
                documentRepository,
                userConfig
        );
        memberAssetService = new MemberAssetServiceImpl(processor, userConfig);
    }

    @Test
    void syncMemberAssets_nullRequest_throwsException() {
        assertThrows(CustomException.class, () -> memberAssetService.syncMemberAssets(null));
    }

    @Test
    void syncMemberAssets_emptyMembers_returnsSuccessWithEmptyResults() {
        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(new RequestInfo())
                .members(Collections.emptyList())
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    void syncMemberAssets_userNotFound_returnsFailedResult() {
        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid("non-existent-uuid")
                .tenantId("pg.citya")
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(new RequestInfo())
                .member(detail)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void syncMemberAssets_tenantMismatch_returnsFailedResult() {
        // Given an existing user in tenant 'pg.citya'
        String citizenUuid = "citizen-uuid-tenant-test";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        // When a payload supplies an incompatible tenant 'pb.amritsar'
        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pb.amritsar") // Incompatible tenant state root
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(new RequestInfo())
                .member(detail)
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        // Then synchronization should fail for that member with tenant mismatch error
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getMessage().contains("Tenant ID mismatch"));
    }

    @Test
    void syncMemberAssets_successfulSyncWithAllAssets() {
        String citizenUuid = "citizen-uuid-001";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .username("9876543210")
                .mobileNumber("9876543210")
                .type(UserType.CITIZEN)
                .fatherName("Old Father")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        CitizenProfileDetail citizenDetails = CitizenProfileDetail.builder()
                .uuid(citizenUuid)
                .fatherName("Ramesh Kumar")
                .motherName("Sita Devi")
                .spouseName("Anita Kumar")
                .caste("OBC")
                .isSpeciallyAbled(false)
                .guardian("Ramesh Kumar")
                .guardianRelation("FATHER")
                .alternateMobileNumber("9876543211")
                .emailId("citizen@example.com")
                .documents(Collections.singletonList(
                        UserDocument.builder()
                                .documentType("CASTE_CERTIFICATE")
                                .filestoreId("file-caste-123")
                                .build()
                ))
                .build();

        // 1 Urban Property
        UserProperty urbanProp = UserProperty.builder()
                .localityType("URBAN")
                .ownershipType("INDIVIDUAL")
                .urbanPropertyDetail(UrbanPropertyDetail.builder()
                        .upyogPropertyId("UPYOG-PROP-001")
                        .propertyTaxAmount(new BigDecimal("1200.00"))
                        .urbanAddress("House 12, Sector 4")
                        .build())
                .document(UserDocument.builder()
                        .documentType("PROPERTY_TAX_RECEIPT")
                        .filestoreId("file-tax-rec-456")
                        .build())
                .build();

        // 1 Rural Property with 1 Joint Owner
        UserProperty ruralProp = UserProperty.builder()
                .localityType("RURAL")
                .ownershipType("JOINT")
                .ruralPropertyDetail(RuralPropertyDetail.builder()
                        .gramPanchayat("Khedi GP")
                        .villageName("Khedi")
                        .wardNumber("4")
                        .surveyNumber("KHASRA-99")
                        .propertyType("AGRICULTURAL")
                        .build())
                .owners(Collections.singletonList(
                        PropertyOwner.builder()
                                .ownerType("JOINT_OWNER")
                                .userUuid("joint-owner-uuid-002")
                                .build()
                ))
                .build();

        // Income
        UserIncome income = UserIncome.builder()
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("650000.00"))
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .salaryProofDocument(UserDocument.builder()
                        .documentType("SALARY_SLIP")
                        .filestoreId("file-salary-789")
                        .build())
                .build();

        // Vehicle
        UserVehicle vehicle = UserVehicle.builder()
                .vehicleNumber("DL01AB1234")
                .purchaseDate("2022-04-15")
                .vehicleType("4_WHEELER")
                .makerModel("Maruti Brezza")
                .fuelType("PETROL")
                .category("LMV")
                .rcDocument(UserDocument.builder()
                        .documentType("RC_BOOK")
                        .filestoreId("file-rc-999")
                        .build())
                .build();

        MemberAssetDetail memberDetail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .familyId("FAM-2026-001")
                .citizenDetails(citizenDetails)
                .properties(List.of(urbanProp, ruralProp))
                .income(income)
                .vehicles(Collections.singletonList(vehicle))
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .members(Collections.singletonList(memberDetail))
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getResults().size());

        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(citizenUuid, result.getUserUuid());
        assertEquals(2, result.getPropertiesSynced());
        assertTrue(result.isIncomeSynced());
        assertEquals(1, result.getVehiclesSynced());

        // Verify demographic update in userRepository
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).update(userCaptor.capture(), any(), anyLong(), anyString());
        User updatedUser = userCaptor.getValue();
        assertEquals("Ramesh Kumar", updatedUser.getFatherName());
        assertEquals("Sita Devi", updatedUser.getMotherName());
        assertEquals("Anita Kumar", updatedUser.getSpouseName());
        assertEquals("OBC", updatedUser.getCaste());
        assertEquals(Boolean.FALSE, updatedUser.getIsSpeciallyAbled());
        assertEquals(GuardianRelation.FATHER, updatedUser.getGuardianRelation());

        // Verify property repository interactions
        verify(propertyRepository, times(2)).saveProperty(any(UserProperty.class));
        verify(propertyRepository, times(1)).saveUrbanProperty(any(UrbanPropertyDetail.class));
        verify(propertyRepository, times(1)).saveRuralProperty(any(RuralPropertyDetail.class));
        verify(propertyRepository, times(1)).savePropertyOwner(any(PropertyOwner.class));

        // Verify income repository interactions
        verify(incomeRepository, times(1)).saveIncome(any(UserIncome.class));

        // Verify vehicle repository interactions
        org.mockito.ArgumentCaptor<UserVehicle> vehicleCaptor = org.mockito.ArgumentCaptor.forClass(UserVehicle.class);
        verify(vehicleRepository, times(1)).saveVehicle(vehicleCaptor.capture());
        assertEquals("PETROL", vehicleCaptor.getValue().getFuelType());
        assertEquals("LMV", vehicleCaptor.getValue().getCategory());

        // Verify documents persisted with referenceType and referenceId
        verify(documentRepository, atLeast(4)).create(any(UserDocument.class));
    }

    @Test
    void syncMemberAssets_idempotency_reusesExistingIdsOnResync() {
        // Given an existing user and pre-existing asset records in the database
        String citizenUuid = "citizen-uuid-resync-001";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        // Mock existing record lookups returning existing IDs
        when(propertyRepository.findUrbanPropertyId(citizenUuid, "UPYOG-PROP-001"))
                .thenReturn("existing-prop-id-123");
        when(incomeRepository.findActiveIncomeIdByUserUuid(citizenUuid))
                .thenReturn("existing-income-id-456");
        when(vehicleRepository.findVehicleIdByUserAndNumber(citizenUuid, "DL01AB1234"))
                .thenReturn("existing-veh-id-789");

        UserProperty urbanProp = UserProperty.builder()
                .localityType("URBAN")
                .urbanPropertyDetail(UrbanPropertyDetail.builder()
                        .upyogPropertyId("UPYOG-PROP-001")
                        .build())
                .build();

        UserIncome income = UserIncome.builder()
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .build();

        UserVehicle vehicle = UserVehicle.builder()
                .vehicleNumber("DL01AB1234")
                .build();

        MemberAssetDetail memberDetail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .properties(Collections.singletonList(urbanProp))
                .income(income)
                .vehicles(Collections.singletonList(vehicle))
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .members(Collections.singletonList(memberDetail))
                .build();

        // When re-syncing assets without client-supplied IDs
        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        // Then verify existing IDs were reused, preserving idempotency
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("existing-income-id-456", result.getIncomeId());
        assertTrue(result.getPropertyIds().contains("existing-prop-id-123"));
        assertTrue(result.getVehicleIds().contains("existing-veh-id-789"));
    }

    @Test
    void syncMemberAssets_foreignIdSupplied_blocksCrossUserOverwrite() {
        // Given an existing user
        String citizenUuid = "citizen-uuid-legit";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        // Mock ownership checks: the provided property ID belongs to a foreign user
        when(propertyRepository.findUserUuidByPropertyId("foreign-prop-id-999"))
                .thenReturn("victim-user-uuid-888");

        UserProperty maliciousProp = UserProperty.builder()
                .id("foreign-prop-id-999") // Client maliciously supplies foreign property ID
                .localityType("URBAN")
                .urbanPropertyDetail(UrbanPropertyDetail.builder()
                        .upyogPropertyId("UPYOG-PROP-ATTACK")
                        .build())
                .build();

        MemberAssetDetail memberDetail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .properties(Collections.singletonList(maliciousProp))
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .members(Collections.singletonList(memberDetail))
                .build();

        // When sync is performed
        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        // Then the foreign property ID is blocked from overwrite and a new UUID is generated
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        MemberAssetSyncResult result = response.getResults().get(0);
        assertFalse(result.getPropertyIds().contains("foreign-prop-id-999"),
                "Foreign property ID must not be reused to prevent cross-user overwrite");
    }

    @Test
    void syncMemberAssets_singleMemberException_setsTopLevelStatusFailedAndSanitizesError() {
        // Given a member whose processing encounters an internal database/SQL exception
        String citizenUuid = "citizen-uuid-crash";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(existingUser));

        // Simulate database exception during property save
        doThrow(new org.springframework.dao.DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint 'pg_catalog.idx_raw_sql'"))
                .when(propertyRepository).saveProperty(any(UserProperty.class));

        UserProperty prop = UserProperty.builder()
                .localityType("URBAN")
                .build();

        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .properties(Collections.singletonList(prop))
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .members(Collections.singletonList(detail))
                .build();

        // When sync is performed
        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        // Then overall status is FAILED (not SUCCESS) and raw SQL text is sanitized
        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertEquals("FAILED", response.getResponseInfo().getStatus());
        assertEquals(1, response.getResults().size());

        MemberAssetSyncResult result = response.getResults().get(0);
        assertEquals("FAILED", result.getStatus());
        assertFalse(result.getMessage().contains("pg_catalog.idx_raw_sql"), "Raw SQL exception text must not leak");
        assertTrue(result.getMessage().contains("internal processing error"));
    }

    @Test
    void syncMemberAssets_partialFailure_setsTopLevelStatusPartialSuccess() {
        // Given two members: member 1 succeeds, member 2 fails with tenant mismatch
        String uuidSuccess = "citizen-uuid-success";
        String uuidFail = "citizen-uuid-fail";

        User userSuccess = User.builder()
                .id(101L)
                .uuid(uuidSuccess)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        User userFail = User.builder()
                .id(102L)
                .uuid(uuidFail)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenAnswer(invocation -> {
                    UserSearchCriteria criteria = invocation.getArgument(0);
                    if (criteria.getUuid() != null && criteria.getUuid().contains(uuidSuccess)) {
                        return Collections.singletonList(userSuccess);
                    } else if (criteria.getUuid() != null && criteria.getUuid().contains(uuidFail)) {
                        return Collections.singletonList(userFail);
                    }
                    return Collections.emptyList();
                });

        MemberAssetDetail detail1 = MemberAssetDetail.builder()
                .userUuid(uuidSuccess)
                .tenantId("pg.citya")
                .build();

        MemberAssetDetail detail2 = MemberAssetDetail.builder()
                .userUuid(uuidFail)
                .tenantId("pb.amritsar") // Incompatible tenant -> will fail
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .members(List.of(detail1, detail2))
                .build();

        // When sync is performed
        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        // Then overall status is PARTIAL_SUCCESS
        assertNotNull(response);
        assertEquals("PARTIAL_SUCCESS", response.getStatus());
        assertEquals("PARTIAL_SUCCESS", response.getResponseInfo().getStatus());
        assertEquals(2, response.getResults().size());

        assertEquals("SUCCESS", response.getResults().get(0).getStatus());
        assertEquals("FAILED", response.getResults().get(1).getStatus());
    }

    @Test
    void syncMemberAssets_auditActor_usesCallerUserInfoWhenPresent() {
        // Fix for PR Comment #4: Ensure caller credentials from RequestInfo.userInfo are strictly used
        String citizenUuid = "citizen-uuid-actor-001";
        User existingUser = User.builder()
                .id(101L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .fatherName("Old Father")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingUser));

        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .fatherName("New Father")
                .build();

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder()
                        .id(777L)
                        .uuid("caller-employee-uuid")
                        .build())
                .build();

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(requestInfo)
                .members(Collections.singletonList(detail))
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        // Verify that userRepository.update received the caller's ID and UUID, not the citizen's
        verify(userRepository).update(any(User.class), any(User.class), eq(777L), eq("caller-employee-uuid"));
    }

    @Test
    void syncMemberAssets_auditActor_usesConfiguredSystemActorWhenUserInfoAbsent() {
        // Fix for PR Comment #4: Missing userInfo must fall back to configured system actor, NEVER citizen's own ID
        String citizenUuid = "citizen-uuid-actor-002";
        User existingUser = User.builder()
                .id(999L)
                .uuid(citizenUuid)
                .tenantId("pg.citya")
                .type(UserType.CITIZEN)
                .fatherName("Old Father")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(existingUser));

        MemberAssetDetail detail = MemberAssetDetail.builder()
                .userUuid(citizenUuid)
                .tenantId("pg.citya")
                .fatherName("New Father")
                .build();

        // RequestInfo without userInfo
        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .members(Collections.singletonList(detail))
                .build();

        MemberAssetSyncResponse response = memberAssetService.syncMemberAssets(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        // Verify that userRepository.update received DEFAULT_SYSTEM_ACTOR_ID (1L) and "SYSTEM", NOT citizen's 999L / citizenUuid
        verify(userRepository).update(any(User.class), any(User.class), eq(1L), eq("SYSTEM"));
    }

    @Test
    void syncMemberAssets_auditActor_rejectsRequestWhenCallerRequiredAndMissing() {
        // Fix for PR Comment #4: When requireCallerForSync is configured, missing userInfo must be rejected
        UserConfiguration config = UserConfiguration.builder()
                .requireCallerForSync(true)
                .build();

        MemberAssetProcessor processor = new MemberAssetProcessor(userRepository, propertyRepository,
                incomeRepository, vehicleRepository, documentRepository, config);
        MemberAssetServiceImpl serviceWithEnforcement = new MemberAssetServiceImpl(processor, config);

        MemberAssetSyncRequest request = MemberAssetSyncRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .members(Collections.singletonList(MemberAssetDetail.builder().userUuid("some-uuid").build()))
                .build();

        CustomException exception = assertThrows(CustomException.class, () ->
                serviceWithEnforcement.syncMemberAssets(request)
        );

        assertEquals("CALLER_INFO_REQUIRED", exception.getCode());
    }
}
