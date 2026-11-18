package org.upyog.family.domain.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.FileStoreClient;
import org.upyog.family.persistence.repository.RequesterDxClient;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.JoinFamilyRequest;
import org.upyog.family.web.contract.RuralPropertyDetail;
import org.upyog.family.web.contract.UrbanPropertyDetail;
import org.upyog.family.web.contract.UserDocument;
import org.upyog.family.web.contract.UserIncomeDetail;
import org.upyog.family.web.contract.UserProperty;
import org.upyog.family.web.contract.UserVehicleDetail;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinFamilyValidatorTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyMasterDataService masterDataService;

    @Mock
    private RequesterDxClient requesterDxClient;

    @Mock
    private UserClient userClient;

    @Mock
    private FileStoreClient fileStoreClient;

    @InjectMocks
    private JoinFamilyValidator validator;

    private Family activeFamily;

    @BeforeEach
    void setUp() {
        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .build();
    }

    @Test
    @DisplayName("Should pass when citizen joins with valid relationship and txnId")
    void validate_Success() {
        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validator.validate(request, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if txnId is missing")
    void validate_MissingTxnId_ThrowsException() {
        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId(null)
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should throw exception if applicant already in an active family")
    void validate_AlreadyInActiveFamily() {
        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5"))
                .thenReturn(Collections.singletonList("FAM-2026-8888"));

        assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
    }

    @Test
    @DisplayName("Should pass when citizen joins declaring valid properties, income, vehicles, and citizenDetails")
    void validate_WithValidAssets_Success() {
        UserProperty urbanProp = UserProperty.builder()
                .localityType(FamilyConstants.LOCALITY_TYPE_URBAN)
                .urbanPropertyDetail(UrbanPropertyDetail.builder()
                        .upyogPropertyId("PG-PT-2026-0012")
                        .build())
                .build();

        UserIncomeDetail income = UserIncomeDetail.builder()
                .occupation("SALARIED_PRIVATE")
                .annualIncome(BigDecimal.valueOf(500000))
                .build();

        UserVehicleDetail vehicle = UserVehicleDetail.builder()
                .vehicleNumber("PB-02-AB-1234")
                .build();

        CitizenProfile citizenDetails = CitizenProfile.builder()
                .caste("GENERAL")
                .build();

        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .citizenDetails(citizenDetails)
                .properties(Collections.singletonList(urbanProp))
                .income(income)
                .vehicles(Collections.singletonList(vehicle))
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validator.validate(request, activeFamily));
        verify(masterDataService).validateCategory(any(), eq("pg.citya"), eq("GENERAL"));
        verify(masterDataService).validateOccupation(any(), eq("pg.citya"), eq("SALARIED_PRIVATE"));
    }

    @Test
    @DisplayName("Should throw exception when rural property lacks Gram Panchayat or Village Name")
    void validate_RuralProperty_MissingGramPanchayat_ThrowsException() {
        UserProperty ruralProp = UserProperty.builder()
                .localityType(FamilyConstants.LOCALITY_TYPE_RURAL)
                .ruralPropertyDetail(RuralPropertyDetail.builder()
                        .gramPanchayat("")
                        .villageName("Rampur")
                        .build())
                .build();

        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .properties(Collections.singletonList(ruralProp))
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should throw exception when urban property lacks UPYOG Property Tax assessment ID")
    void validate_UrbanProperty_MissingUpyogPropertyId_ThrowsException() {
        UserProperty urbanProp = UserProperty.builder()
                .localityType(FamilyConstants.LOCALITY_TYPE_URBAN)
                .urbanPropertyDetail(UrbanPropertyDetail.builder()
                        .upyogPropertyId(null)
                        .build())
                .build();

        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .properties(Collections.singletonList(urbanProp))
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should throw exception when vehicle lacks vehicleNumber")
    void validate_Vehicle_MissingVehicleNumber_ThrowsException() {
        UserVehicleDetail vehicle = UserVehicleDetail.builder()
                .vehicleNumber("")
                .build();

        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .vehicles(Collections.singletonList(vehicle))
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should throw exception when attached proof document does not exist in FileStore")
    void validate_DocumentNotFoundInFileStore_ThrowsException() {
        CitizenProfile citizenDetails = CitizenProfile.builder()
                .documents(Collections.singletonList(UserDocument.builder()
                        .filestoreId("missing-file-id")
                        .build()))
                .build();

        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .citizenDetails(citizenDetails)
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());
        when(fileStoreClient.validateFileExists("pg.citya", "missing-file-id")).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
        assertEquals(FamilyConstants.ERROR_FILESTORE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("Should throw exception when document has blank filestoreId")
    void validate_DocumentWithBlankFilestoreId_ThrowsException() {
        CitizenProfile citizenDetails = CitizenProfile.builder()
                .documents(Collections.singletonList(UserDocument.builder()
                        .filestoreId("   ")
                        .build()))
                .build();

        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .citizenDetails(citizenDetails)
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
        assertEquals(FamilyConstants.ERROR_FILESTORE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("Should throw exception when declared annual income is negative")
    void validate_NegativeAnnualIncome_ThrowsException() {
        UserIncomeDetail income = UserIncomeDetail.builder()
                .annualIncome(BigDecimal.valueOf(-1000))
                .build();

        JoinFamilyRequest request = JoinFamilyRequest.builder()
                .requestInfo(new RequestInfo())
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .applicantUserUuid("applicant-uuid-5")
                .relationshipWithHead("SON")
                .txnId("OTP-1234")
                .income(income)
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("applicant-uuid-5")).thenReturn(Collections.emptyList());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(request, activeFamily));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }
}
