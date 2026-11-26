package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.persistence.repository.UserVehicleRepository;
import org.egov.user.web.contract.asset.VehicleRequest;
import org.egov.user.web.contract.asset.VehicleSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying business validations, security boundaries, and persistence workflows in {@link VehicleServiceImpl}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class VehicleServiceImplTest {

    private UserVehicleRepository vehicleRepository;
    private UserRepository userRepository;
    private UserDocumentRepository documentRepository;
    private org.egov.user.persistence.repository.FamilyRestRepository familyRestRepository;
    private AssetAuthorizationService assetAuthorizationService;
    private VehicleServiceImpl vehicleService;

    private static final String TEST_USER_UUID = "citizen-uuid-001";
    private static final String TEST_MEMBER_UUID = "member-uuid-002";
    private static final String TEST_VEHICLE_ID = "veh-001";
    private static final String TEST_TENANT_ID = "pg.citya";
    private static final String TEST_OPERATOR_UUID = "operator-uuid-555";
    private static final Long TEST_OPERATOR_ID = 50L;

    @BeforeEach
    public void setUp() {
        vehicleRepository = mock(UserVehicleRepository.class);
        userRepository = mock(UserRepository.class);
        documentRepository = mock(UserDocumentRepository.class);
        familyRestRepository = mock(org.egov.user.persistence.repository.FamilyRestRepository.class);

        assetAuthorizationService = new AssetAuthorizationService(userRepository, familyRestRepository);

        vehicleService = new VehicleServiceImpl(
                vehicleRepository,
                userRepository,
                documentRepository,
                assetAuthorizationService
        );
    }

    private RequestInfo createTestRequestInfo() {
        org.egov.common.contract.request.User userInfo = org.egov.common.contract.request.User.builder()
                .uuid(TEST_OPERATOR_UUID)
                .id(TEST_OPERATOR_ID)
                .type("EMPLOYEE")
                .roles(Collections.singletonList(org.egov.common.contract.request.Role.builder().code("EMPLOYEE").name("Employee").build()))
                .build();
        return RequestInfo.builder()
                .userInfo(userInfo)
                .build();
    }

    private RequestInfo createCitizenRequestInfo(String citizenUuid) {
        org.egov.common.contract.request.User userInfo = org.egov.common.contract.request.User.builder()
                .uuid(citizenUuid)
                .id(101L)
                .type("CITIZEN")
                .roles(Collections.singletonList(org.egov.common.contract.request.Role.builder().code("CITIZEN").name("Citizen").build()))
                .build();
        return RequestInfo.builder()
                .userInfo(userInfo)
                .build();
    }

    @Test
    @DisplayName("Should create vehicle successfully when valid request is provided")
    public void testCreateVehicle_Success() {
        User citizen = User.builder()
                .id(101L)
                .uuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();
        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(citizen));

        UserVehicle savedVehicle = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .makerModel("Tata Nexon EV")
                .fuelType("ELECTRIC")
                .category("LMV")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(vehicleRepository.findVehicleById(any())).thenReturn(savedVehicle);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .makerModel("Tata Nexon EV")
                .fuelType("ELECTRIC")
                .category("LMV")
                .build();

        UserVehicle created = vehicleService.createVehicle(request);

        assertNotNull(created);
        assertEquals(TEST_VEHICLE_ID, created.getId());
        assertEquals("DL01AB1234", created.getVehicleNumber());

        ArgumentCaptor<UserVehicle> captor = ArgumentCaptor.forClass(UserVehicle.class);
        verify(vehicleRepository, times(1)).saveVehicle(captor.capture());
        UserVehicle persisted = captor.getValue();
        assertEquals(TEST_OPERATOR_UUID, persisted.getCreatedBy());
        assertEquals(TEST_OPERATOR_UUID, persisted.getLastModifiedBy());
        assertEquals(UserConstants.STATUS_ACTIVE_UPPERCASE, persisted.getStatus());
    }

    @Test
    @DisplayName("Should create vehicle and persist attached RC document proof")
    public void testCreateVehicle_WithRcDocument() {
        User citizen = User.builder()
                .id(101L)
                .uuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();
        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(citizen));

        UserDocument rcDoc = UserDocument.builder()
                .id("rc-doc-uuid-1")
                .filestoreId("file-store-rc-99")
                .documentType("REGISTRATION_CERTIFICATE")
                .build();

        UserVehicle savedVehicle = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .vehicleNumber("DL01AB1234")
                .rcDocId("rc-doc-uuid-1")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(vehicleRepository.findVehicleById(any())).thenReturn(savedVehicle);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .rcDocument(rcDoc)
                .build();

        UserVehicle created = vehicleService.createVehicle(request);

        assertNotNull(created);
        ArgumentCaptor<UserDocument> docCaptor = ArgumentCaptor.forClass(UserDocument.class);
        verify(documentRepository, times(1)).create(docCaptor.capture());
        UserDocument persistedDoc = docCaptor.getValue();
        assertEquals(UserConstants.DOC_REF_TYPE_VEHICLE_RC, persistedDoc.getReferenceType());
        assertEquals(TEST_OPERATOR_UUID, persistedDoc.getCreatedBy());
        assertEquals(TEST_OPERATOR_UUID, persistedDoc.getLastModifiedBy());
    }

    @Test
    @DisplayName("Should deduplicate existing vehicle by vehicle registration number if ID is not passed")
    public void testCreateVehicle_NaturalKeyDeduplication() {
        User citizen = User.builder()
                .id(101L)
                .uuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();
        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(citizen));
        when(vehicleRepository.findVehicleIdByUserAndNumber(TEST_USER_UUID, "DL01AB1234"))
                .thenReturn("existing-veh-id-888");

        UserVehicle savedVehicle = UserVehicle.builder()
                .id("existing-veh-id-888")
                .userUuid(TEST_USER_UUID)
                .vehicleNumber("DL01AB1234")
                .build();
        when(vehicleRepository.findVehicleById("existing-veh-id-888")).thenReturn(savedVehicle);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .build();

        UserVehicle created = vehicleService.createVehicle(request);

        assertNotNull(created);
        assertEquals("existing-veh-id-888", created.getId());
    }

    @Test
    @DisplayName("Should throw CustomException when vehicle is already actively mapped to another citizen")
    public void testCreateVehicle_AlreadyMappedToAnotherCitizen_ThrowsException() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).tenantId(TEST_TENANT_ID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));
        when(vehicleRepository.findActiveOwnerByVehicleNumber("DL01AB1234")).thenReturn("another-citizen-uuid-999");

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.createVehicle(request));
        assertEquals(UserConstants.ERROR_VEHICLE_ALREADY_REGISTERED, ex.getCode());
        verify(vehicleRepository, never()).saveVehicle(any());
    }

    @Test
    @DisplayName("Should throw CustomException when vehicle creation request is null")
    public void testCreateVehicle_NullRequest_ThrowsException() {
        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.createVehicle(null));
        assertEquals(UserConstants.ERROR_INVALID_VEHICLE_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when userUuid is missing")
    public void testCreateVehicle_MissingUserUuid_ThrowsException() {
        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.createVehicle(request));
        assertEquals(UserConstants.ERROR_USER_UUID_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when citizen does not exist in eg_user")
    public void testCreateVehicle_CitizenNotFound_ThrowsException() {
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.createVehicle(request));
        assertEquals(UserConstants.ERROR_USER_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when mandatory vehicle fields are missing")
    public void testCreateVehicle_MissingMandatoryFields_ThrowsException() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).tenantId(TEST_TENANT_ID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("") // blank vehicle number
                .vehicleType("4_WHEELER")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.createVehicle(request));
        assertEquals(UserConstants.ERROR_VEHICLE_MANDATORY_FIELDS, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when RequestInfo caller userInfo is missing")
    public void testCreateVehicle_MissingCallerInfo_ThrowsException() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).tenantId(TEST_TENANT_ID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(RequestInfo.builder().build()) // empty userInfo
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.createVehicle(request));
        assertEquals(UserConstants.ERROR_CALLER_INFO_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("Should update vehicle attributes successfully")
    public void testUpdateVehicle_Success() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .makerModel("Old Model")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .makerModel("New Model 2026")
                .fuelType("PETROL")
                .build();

        UserVehicle updated = vehicleService.updateVehicle(request);

        assertNotNull(updated);
        assertEquals("New Model 2026", existing.getMakerModel());
        assertEquals("PETROL", existing.getFuelType());
        assertEquals(TEST_OPERATOR_UUID, existing.getLastModifiedBy());
        verify(vehicleRepository, times(1)).saveVehicle(existing);
    }

    @Test
    @DisplayName("Should throw CustomException when updating non-existent vehicle")
    public void testUpdateVehicle_NotFound_ThrowsException() {
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(null);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.updateVehicle(request));
        assertEquals(UserConstants.ERROR_VEHICLE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("Should reject unauthorized cross-citizen vehicle update attempt")
    public void testUpdateVehicle_UnauthorizedCrossCitizen_ThrowsException() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid("citizen-original-owner")
                .tenantId(TEST_TENANT_ID)
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid("citizen-attacker-uuid")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.updateVehicle(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
        verify(vehicleRepository, never()).saveVehicle(any());
    }

    @Test
    @DisplayName("Should reject vehicle number update if new plate number is already actively registered to another citizen")
    public void testUpdateVehicle_NumberAlreadyMappedToAnotherCitizen_ThrowsException() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);
        when(vehicleRepository.findActiveOwnerByVehicleNumber("DL02CD5678")).thenReturn("another-citizen-uuid-999");

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .vehicleNumber("DL02CD5678")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.updateVehicle(request));
        assertEquals(UserConstants.ERROR_VEHICLE_ALREADY_REGISTERED, ex.getCode());
        verify(vehicleRepository, never()).saveVehicle(any());
    }

    @Test
    @DisplayName("Should search vehicles by criteria successfully")
    public void testSearchVehicles_Success() {
        UserVehicle veh = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .build();
        when(vehicleRepository.searchVehicles(any(VehicleSearchCriteria.class)))
                .thenReturn(Collections.singletonList(veh));

        VehicleSearchCriteria criteria = VehicleSearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .build();

        List<UserVehicle> result = vehicleService.searchVehicles(criteria, createTestRequestInfo());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_VEHICLE_ID, result.get(0).getId());
    }

    @Test
    @DisplayName("Should soft-deactivate vehicle by setting status to INACTIVE")
    public void testDeleteVehicle_Success() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        UserVehicle deactivated = vehicleService.deleteVehicle(request);

        assertNotNull(deactivated);
        assertEquals(UserConstants.STATUS_INACTIVE_UPPERCASE, deactivated.getStatus());
        assertEquals(TEST_OPERATOR_UUID, deactivated.getLastModifiedBy());
        verify(vehicleRepository, times(1)).saveVehicle(existing);
    }

    @Test
    @DisplayName("Should reject unauthorized cross-citizen vehicle deletion attempt")
    public void testDeleteVehicle_UnauthorizedCrossCitizen_ThrowsException() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid("citizen-original-owner")
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createTestRequestInfo())
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid("citizen-attacker-uuid")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.deleteVehicle(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
        verify(vehicleRepository, never()).saveVehicle(any());
    }

    @Test
    @DisplayName("Should permit citizen to create own vehicle declaration")
    public void testCreateVehicle_CitizenSelfAccess_Success() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).tenantId(TEST_TENANT_ID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));

        UserVehicle savedVehicle = UserVehicle.builder()
                .id(TEST_VEHICLE_ID).userUuid(TEST_USER_UUID).vehicleNumber("DL01AB1234")
                .vehicleType("2_WHEELER").makerModel("Hero Splendor").fuelType("PETROL")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE).build();
        when(vehicleRepository.findVehicleById(any())).thenReturn(savedVehicle);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createCitizenRequestInfo(TEST_USER_UUID))
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("2_WHEELER")
                .build();

        UserVehicle created = vehicleService.createVehicle(request);
        assertNotNull(created);
        assertEquals(TEST_USER_UUID, created.getUserUuid());
    }

    @Test
    @DisplayName("Should permit Head of Family to create vehicle declaration for household member")
    public void testCreateVehicle_HeadOfFamily_Success() {
        User member = User.builder().id(102L).uuid(TEST_MEMBER_UUID).tenantId(TEST_TENANT_ID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(member));
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(TEST_USER_UUID), eq(TEST_MEMBER_UUID), eq(TEST_TENANT_ID), any()))
                .thenReturn(true);

        UserVehicle savedVehicle = UserVehicle.builder()
                .id(TEST_VEHICLE_ID).userUuid(TEST_MEMBER_UUID).vehicleNumber("DL02CD5678")
                .vehicleType("2_WHEELER").status(UserConstants.STATUS_ACTIVE_UPPERCASE).build();
        when(vehicleRepository.findVehicleById(any())).thenReturn(savedVehicle);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createCitizenRequestInfo(TEST_USER_UUID))
                .userUuid(TEST_MEMBER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL02CD5678")
                .vehicleType("2_WHEELER")
                .build();

        UserVehicle created = vehicleService.createVehicle(request);
        assertNotNull(created);
        assertEquals(TEST_MEMBER_UUID, created.getUserUuid());
    }

    @Test
    @DisplayName("Should reject vehicle creation when citizen caller is NOT Head of Family")
    public void testCreateVehicle_UnauthorizedCitizen_ThrowsException() {
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(TEST_USER_UUID), eq("stranger-uuid"), eq(TEST_TENANT_ID), any()))
                .thenReturn(false);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createCitizenRequestInfo(TEST_USER_UUID))
                .userUuid("stranger-uuid")
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL03EF9012")
                .vehicleType("4_WHEELER")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.createVehicle(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("Should auto-scope unconstrained citizen vehicle search to caller UUID")
    public void testSearchVehicles_CitizenSelfSearch_AutoScoped() {
        VehicleSearchCriteria criteria = VehicleSearchCriteria.builder()
                .tenantId(TEST_TENANT_ID)
                .build();

        when(vehicleRepository.searchVehicles(any(VehicleSearchCriteria.class)))
                .thenAnswer(invocation -> {
                    VehicleSearchCriteria passed = invocation.getArgument(0);
                    assertEquals(TEST_USER_UUID, passed.getUserUuid());
                    return Collections.singletonList(UserVehicle.builder().id(TEST_VEHICLE_ID).userUuid(TEST_USER_UUID).build());
                });

        List<UserVehicle> results = vehicleService.searchVehicles(criteria, createCitizenRequestInfo(TEST_USER_UUID));
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Should reject citizen attacker attempting to update another citizen's vehicle even if victim's userUuid is sent in payload")
    public void testUpdateVehicle_AttackerCitizenSendingVictimUserUuid_ThrowsUnauthorizedAccess() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid("citizen-victim-uuid")
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq("citizen-attacker-uuid"), eq("citizen-victim-uuid"), eq(TEST_TENANT_ID), any()))
                .thenReturn(false);

        // Attacker caller puts victim's userUuid in the payload to match existing owner
        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createCitizenRequestInfo("citizen-attacker-uuid"))
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid("citizen-victim-uuid")
                .vehicleNumber("DL01AB1234")
                .makerModel("Hacked Model")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.updateVehicle(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
        verify(vehicleRepository, never()).saveVehicle(any());
    }

    @Test
    @DisplayName("Should reject citizen attacker attempting to deactivate another citizen's vehicle even if victim's userUuid is sent in payload")
    public void testDeleteVehicle_AttackerCitizenSendingVictimUserUuid_ThrowsUnauthorizedAccess() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid("citizen-victim-uuid")
                .tenantId(TEST_TENANT_ID)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq("citizen-attacker-uuid"), eq("citizen-victim-uuid"), eq(TEST_TENANT_ID), any()))
                .thenReturn(false);

        // Attacker caller puts victim's userUuid in the payload to match existing owner
        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createCitizenRequestInfo("citizen-attacker-uuid"))
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid("citizen-victim-uuid")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> vehicleService.deleteVehicle(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
        verify(vehicleRepository, never()).saveVehicle(any());
    }

    @Test
    @DisplayName("Should permit Head of Family to update vehicle declaration of a household member")
    public void testUpdateVehicle_HeadOfFamily_Success() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_MEMBER_UUID)
                .tenantId(TEST_TENANT_ID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .makerModel("Old Model")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(TEST_USER_UUID), eq(TEST_MEMBER_UUID), eq(TEST_TENANT_ID), any()))
                .thenReturn(true);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createCitizenRequestInfo(TEST_USER_UUID))
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid(TEST_MEMBER_UUID)
                .makerModel("HOF Updated Model")
                .build();

        UserVehicle updated = vehicleService.updateVehicle(request);
        assertNotNull(updated);
        assertEquals("HOF Updated Model", existing.getMakerModel());
        assertEquals(TEST_USER_UUID, existing.getLastModifiedBy());
        verify(vehicleRepository, times(1)).saveVehicle(existing);
    }

    @Test
    @DisplayName("Should permit Head of Family to deactivate vehicle declaration of a household member")
    public void testDeleteVehicle_HeadOfFamily_Success() {
        UserVehicle existing = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_MEMBER_UUID)
                .tenantId(TEST_TENANT_ID)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(vehicleRepository.findVehicleById(TEST_VEHICLE_ID)).thenReturn(existing);
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(TEST_USER_UUID), eq(TEST_MEMBER_UUID), eq(TEST_TENANT_ID), any()))
                .thenReturn(true);

        VehicleRequest request = VehicleRequest.builder()
                .requestInfo(createCitizenRequestInfo(TEST_USER_UUID))
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid(TEST_MEMBER_UUID)
                .build();

        UserVehicle deactivated = vehicleService.deleteVehicle(request);
        assertNotNull(deactivated);
        assertEquals(UserConstants.STATUS_INACTIVE_UPPERCASE, deactivated.getStatus());
        assertEquals(TEST_USER_UUID, deactivated.getLastModifiedBy());
        verify(vehicleRepository, times(1)).saveVehicle(existing);
    }
}
