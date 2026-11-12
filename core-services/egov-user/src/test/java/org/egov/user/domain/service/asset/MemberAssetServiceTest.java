package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
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
        memberAssetService = new MemberAssetServiceImpl(
                userRepository,
                propertyRepository,
                incomeRepository,
                vehicleRepository,
                documentRepository
        );
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
        verify(vehicleRepository, times(1)).saveVehicle(any(UserVehicle.class));

        // Verify documents persisted with referenceType and referenceId
        verify(documentRepository, atLeast(4)).create(any(UserDocument.class));
    }
}
