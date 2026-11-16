package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.PropertyOwner;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserPropertyRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.asset.RuralPropertyRequest;
import org.egov.user.web.contract.asset.RuralPropertySearchCriteria;
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
 * Unit tests verifying business validations, security boundaries, and persistence workflows in {@link RuralPropertyServiceImpl}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class RuralPropertyServiceImplTest {

    private UserPropertyRepository propertyRepository;
    private UserRepository userRepository;
    private UserDocumentRepository documentRepository;
    private RuralPropertyServiceImpl ruralPropertyService;

    private static final String TEST_USER_UUID = "citizen-uuid-001";
    private static final String TEST_PROPERTY_ID = "prop-rural-001";
    private static final String TEST_TENANT_ID = "pg.citya";
    private static final String TEST_OPERATOR_UUID = "operator-uuid-555";
    private static final Long TEST_OPERATOR_ID = 50L;

    @BeforeEach
    public void setUp() {
        propertyRepository = mock(UserPropertyRepository.class);
        userRepository = mock(UserRepository.class);
        documentRepository = mock(UserDocumentRepository.class);

        ruralPropertyService = new RuralPropertyServiceImpl(
                propertyRepository,
                userRepository,
                documentRepository
        );
    }

    private RequestInfo createTestRequestInfo() {
        org.egov.common.contract.request.User userInfo = org.egov.common.contract.request.User.builder()
                .uuid(TEST_OPERATOR_UUID)
                .id(TEST_OPERATOR_ID)
                .build();
        return RequestInfo.builder()
                .userInfo(userInfo)
                .build();
    }

    @Test
    @DisplayName("Should create rural property successfully when valid request is provided")
    public void testCreateRuralProperty_Success() {
        User citizen = User.builder()
                .id(101L)
                .uuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();
        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(citizen));

        RuralPropertyDetail ruralDetail = RuralPropertyDetail.builder()
                .gramPanchayat("Rampur")
                .villageName("Fatehpur")
                .surveyNumber("KHASRA-102")
                .propertyType("RESIDENTIAL")
                .propertyUsage("SELF_OCCUPIED")
                .build();

        PropertyOwner jointOwner = PropertyOwner.builder()
                .ownerType("JOINT_OWNER")
                .userUuid("joint-user-002")
                .build();

        UserDocument doc = UserDocument.builder()
                .filestoreId("file-uuid-123")
                .documentType("LAND_TITLE")
                .build();

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .ruralProperty(ruralDetail)
                .document(doc)
                .owners(Collections.singletonList(jointOwner))
                .build();

        UserProperty hydratedReturn = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .localityType(UserConstants.LOCALITY_TYPE_RURAL)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .ruralPropertyDetail(ruralDetail)
                .owners(Collections.singletonList(jointOwner))
                .build();

        when(propertyRepository.findRuralPropertyById(any(String.class))).thenReturn(hydratedReturn);

        UserProperty created = ruralPropertyService.createRuralProperty(request);

        assertNotNull(created);
        assertEquals(TEST_PROPERTY_ID, created.getId());
        assertEquals(TEST_USER_UUID, created.getUserUuid());
        assertEquals(UserConstants.STATUS_ACTIVE_UPPERCASE, created.getStatus());

        verify(propertyRepository, times(1)).saveProperty(any(UserProperty.class));
        verify(propertyRepository, times(1)).saveRuralProperty(any(RuralPropertyDetail.class));

        ArgumentCaptor<UserDocument> docCaptor = ArgumentCaptor.forClass(UserDocument.class);
        verify(documentRepository, times(1)).create(docCaptor.capture());
        assertEquals(TEST_OPERATOR_UUID, docCaptor.getValue().getCreatedBy());
        assertEquals(TEST_OPERATOR_UUID, docCaptor.getValue().getLastModifiedBy());

        verify(propertyRepository, times(1)).savePropertyOwner(any(PropertyOwner.class));
    }

    @Test
    @DisplayName("Should throw CustomException when userUuid is missing during creation")
    public void testCreateRuralProperty_MissingUserUuid() {
        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .tenantId(TEST_TENANT_ID)
                .ruralProperty(RuralPropertyDetail.builder().gramPanchayat("GP").villageName("V").build())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> ruralPropertyService.createRuralProperty(request));
        assertEquals(UserConstants.ERROR_USER_UUID_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when citizen does not exist in eg_user")
    public void testCreateRuralProperty_CitizenNotFound() {
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .userUuid("unknown-uuid")
                .tenantId(TEST_TENANT_ID)
                .ruralProperty(RuralPropertyDetail.builder().gramPanchayat("GP").villageName("V").build())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> ruralPropertyService.createRuralProperty(request));
        assertEquals(UserConstants.ERROR_USER_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when Gram Panchayat or Village Name is missing")
    public void testCreateRuralProperty_MissingMandatoryRuralFields() {
        User citizen = User.builder()
                .id(101L).uuid(TEST_USER_UUID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(citizen));

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .ruralProperty(RuralPropertyDetail.builder().gramPanchayat("GP").villageName(null).build())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> ruralPropertyService.createRuralProperty(request));
        assertEquals(UserConstants.ERROR_RURAL_PROPERTY_MANDATORY_FIELDS, ex.getCode());
    }

    @Test
    @DisplayName("Should update existing rural property and enforce ownership boundary")
    public void testUpdateRuralProperty_Success() {
        UserProperty existing = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .localityType(UserConstants.LOCALITY_TYPE_RURAL)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .ruralPropertyDetail(RuralPropertyDetail.builder()
                        .propertyRecordId(TEST_PROPERTY_ID)
                        .gramPanchayat("Old GP")
                        .villageName("Old Village")
                        .build())
                .build();

        when(propertyRepository.findRuralPropertyById(TEST_PROPERTY_ID)).thenReturn(existing);

        RuralPropertyRequest updateRequest = RuralPropertyRequest.builder()
                .requestInfo(createTestRequestInfo())
                .propertyId(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .ruralProperty(RuralPropertyDetail.builder()
                        .gramPanchayat("New GP")
                        .villageName("New Village")
                        .surveyNumber("KHASRA-UPDATED")
                        .build())
                .build();

        UserProperty updated = ruralPropertyService.updateRuralProperty(updateRequest);

        assertNotNull(updated);
        verify(propertyRepository, times(1)).saveProperty(any(UserProperty.class));
        verify(propertyRepository, times(1)).saveRuralProperty(any(RuralPropertyDetail.class));
    }

    @Test
    @DisplayName("Should reject update if property belongs to a different citizen")
    public void testUpdateRuralProperty_UnauthorizedCrossUserAccess() {
        UserProperty existing = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid("different-citizen-uuid")
                .build();

        when(propertyRepository.findRuralPropertyById(TEST_PROPERTY_ID)).thenReturn(existing);

        RuralPropertyRequest updateRequest = RuralPropertyRequest.builder()
                .requestInfo(createTestRequestInfo())
                .propertyId(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .ruralProperty(RuralPropertyDetail.builder().gramPanchayat("GP").villageName("V").build())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> ruralPropertyService.updateRuralProperty(updateRequest));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
        verify(propertyRepository, never()).saveProperty(any());
    }

    @Test
    @DisplayName("Should search rural properties matching query filters")
    public void testSearchRuralProperties_Success() {
        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();

        UserProperty mockProp = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(propertyRepository.searchRuralProperties(any(RuralPropertySearchCriteria.class)))
                .thenReturn(Collections.singletonList(mockProp));

        List<UserProperty> results = ruralPropertyService.searchRuralProperties(criteria, new RequestInfo());

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(TEST_PROPERTY_ID, results.get(0).getId());
    }

    @Test
    @DisplayName("Should reject search when no query filters are specified")
    public void testSearchRuralProperties_EmptyCriteria() {
        RuralPropertySearchCriteria criteria = new RuralPropertySearchCriteria();

        CustomException ex = assertThrows(CustomException.class,
                () -> ruralPropertyService.searchRuralProperties(criteria, new RequestInfo()));
        assertEquals(UserConstants.ERROR_INVALID_SEARCH_CRITERIA, ex.getCode());
    }

    @Test
    @DisplayName("Should soft-deactivate rural property and mark status as INACTIVE")
    public void testDeleteRuralProperty_Success() {
        UserProperty existing = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        UserProperty deactivated = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .status(UserConstants.STATUS_INACTIVE_UPPERCASE)
                .build();

        when(propertyRepository.findRuralPropertyById(TEST_PROPERTY_ID))
                .thenReturn(existing)
                .thenReturn(deactivated);

        when(propertyRepository.updatePropertyStatus(eq(TEST_PROPERTY_ID), eq(TEST_USER_UUID),
                eq(UserConstants.STATUS_INACTIVE_UPPERCASE), any(String.class))).thenReturn(true);

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .requestInfo(createTestRequestInfo())
                .propertyId(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        UserProperty result = ruralPropertyService.deleteRuralProperty(request);

        assertNotNull(result);
        assertEquals(UserConstants.STATUS_INACTIVE_UPPERCASE, result.getStatus());

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(propertyRepository, times(1)).updatePropertyStatus(
                eq(TEST_PROPERTY_ID),
                eq(TEST_USER_UUID),
                statusCaptor.capture(),
                any(String.class)
        );
        assertEquals(UserConstants.STATUS_INACTIVE_UPPERCASE, statusCaptor.getValue());
    }

    @Test
    @DisplayName("Should reject deactivation when property belongs to a different citizen")
    public void testDeactivateRuralProperty_UnauthorizedCrossUserAccess() {
        UserProperty existing = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid("other-user-uuid")
                .build();

        when(propertyRepository.findRuralPropertyById(TEST_PROPERTY_ID)).thenReturn(existing);

        CustomException ex = assertThrows(CustomException.class,
                () -> ruralPropertyService.deactivateRuralProperty(TEST_PROPERTY_ID, TEST_USER_UUID, createTestRequestInfo()));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
        verify(propertyRepository, never()).updatePropertyStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw CustomException when userInfo is absent during creation")
    public void testCreateRuralProperty_CallerMissing() {
        User citizen = User.builder()
                .id(101L)
                .uuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();
        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(citizen));

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .requestInfo(new RequestInfo()) // Missing userInfo
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .ruralProperty(RuralPropertyDetail.builder()
                        .gramPanchayat("GP")
                        .villageName("Village")
                        .build())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> ruralPropertyService.createRuralProperty(request));
        assertEquals(UserConstants.ERROR_CALLER_INFO_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when userInfo is absent during update")
    public void testUpdateRuralProperty_CallerMissing() {
        UserProperty existing = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .build();
        when(propertyRepository.findRuralPropertyById(TEST_PROPERTY_ID)).thenReturn(existing);

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .requestInfo(new RequestInfo())
                .propertyId(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .ruralProperty(RuralPropertyDetail.builder().gramPanchayat("GP").villageName("V").build())
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> ruralPropertyService.updateRuralProperty(request));
        assertEquals(UserConstants.ERROR_CALLER_INFO_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when userInfo is absent during deactivation")
    public void testDeactivateRuralProperty_CallerMissing() {
        UserProperty existing = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .build();
        when(propertyRepository.findRuralPropertyById(TEST_PROPERTY_ID)).thenReturn(existing);

        CustomException ex = assertThrows(CustomException.class,
                () -> ruralPropertyService.deactivateRuralProperty(TEST_PROPERTY_ID, TEST_USER_UUID, new RequestInfo()));
        assertEquals(UserConstants.ERROR_CALLER_INFO_REQUIRED, ex.getCode());
    }
}
