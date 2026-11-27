package org.upyog.family.domain.validator;

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
import org.upyog.family.web.contract.AddMemberRequest;
import org.upyog.family.web.contract.CitizenProfile;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.UrbanPropertyDetail;
import org.upyog.family.web.contract.UserDocument;
import org.upyog.family.web.contract.UserIncomeDetail;
import org.upyog.family.web.contract.UserProperty;
import org.upyog.family.web.contract.UserVehicleDetail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddMemberValidatorTest {

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
    private AddMemberValidator validator;

    private Family activeFamily;
    private AddMemberRequest validRequest;

    @BeforeEach
    void setUp() {
        FamilyMember head = FamilyMember.builder()
                .userUuid("head-uuid-1")
                .isHead(true)
                .active(true)
                .build();

        activeFamily = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .status(FamilyStatus.ACTIVE)
                .headUserUuid("head-uuid-1")
                .members(new ArrayList<>(Collections.singletonList(head)))
                .build();

        FamilyMember newMember = FamilyMember.builder()
                .userUuid("member-uuid-2")
                .relationshipWithHead("SPOUSE")
                .isHead(false)
                .active(true)
                .build();

        validRequest = AddMemberRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .member(newMember)
                .consentToken("CONSENT-VERIFIED-12345")
                .build();
    }

    @Test
    @DisplayName("Should pass validation when all rules and Aadhaar consent succeed")
    void validate_Success() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(validRequest, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if family does not exist")
    void validate_FamilyNotFound() {
        assertThrows(CustomException.class, () -> validator.validate(validRequest, null));
    }

    @Test
    @DisplayName("Should throw exception if family is not active")
    void validate_FamilyInactive() {
        activeFamily.setStatus(FamilyStatus.SPLIT);
        assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if citizen is already active in another family")
    void validate_CitizenAlreadyInAnotherFamily() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2"))
                .thenReturn(Collections.singletonList("FAM-2026-9999"));

        assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
    }

    @Test
    @DisplayName("Should pass validation when adding minor under HoF guardian without adult OTP token")
    void validate_MinorUnderGuardian_Success() {
        FamilyMember child = FamilyMember.builder()
                .userUuid("child-uuid-3")
                .relationshipWithHead("CHILD")
                .isHead(false)
                .active(true)
                .build();

        AddMemberRequest childRequest = AddMemberRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .member(child)
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("child-uuid-3")).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validator.validate(childRequest, activeFamily));
    }

    @Test
    @DisplayName("Should throw exception if Aadhaar consent is invalid")
    void validate_ConsentInvalid() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(false);

        assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
    }

    @Test
    @DisplayName("Should pass when adding member with valid 3NF assets and citizen details")
    void validate_WithValidAssets_Success() {
        UserProperty urbanProp = UserProperty.builder()
                .localityType(FamilyConstants.LOCALITY_TYPE_URBAN)
                .ownershipType("INDIVIDUAL")
                .consentToken("CONSENT-VERIFIED-12345")
                .urbanPropertyDetail(UrbanPropertyDetail.builder()
                        .upyogPropertyId("PG-PT-2026-0099")
                        .consentToken("CONSENT-VERIFIED-12345")
                        .build())
                .build();

        UserIncomeDetail income = UserIncomeDetail.builder()
                .occupation("SALARIED_PRIVATE")
                .annualIncome(BigDecimal.valueOf(600000))
                .build();

        UserVehicleDetail vehicle = UserVehicleDetail.builder()
                .vehicleNumber("DL-01-AB-1234")
                .build();

        CitizenProfile citizenDetails = CitizenProfile.builder()
                .caste("OBC")
                .build();

        validRequest.getMember().setProperties(Collections.singletonList(urbanProp));
        validRequest.getMember().setIncome(income);
        validRequest.getMember().setVehicles(Collections.singletonList(vehicle));
        validRequest.getMember().setCitizenDetails(citizenDetails);

        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);
        when(userClient.validatePropertyConsentToken(eq("PG-PT-2026-0099"), eq("CONSENT-VERIFIED-12345"), any())).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(validRequest, activeFamily));
        verify(masterDataService).validateCategory(any(), eq("pg.citya"), eq("OBC"));
        verify(masterDataService).validateOccupation(any(), eq("pg.citya"), eq("SALARIED_PRIVATE"));
        verify(masterDataService).validateOwnershipType(any(), eq("pg.citya"), eq("INDIVIDUAL"));
    }

    @Test
    @DisplayName("Should throw exception when added member property lacks ownershipType")
    void validate_PropertyMissingOwnershipType_ThrowsException() {
        UserProperty prop = UserProperty.builder()
                .localityType(FamilyConstants.LOCALITY_TYPE_URBAN)
                .ownershipType(null)
                .build();

        validRequest.getMember().setProperties(Collections.singletonList(prop));

        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);
        doThrow(new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_PROPERTY_OWNERSHIP_TYPE_MANDATORY))
                .when(masterDataService).validateOwnershipType(any(), eq("pg.citya"), eq(null));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
        assertEquals(FamilyConstants.MSG_PROPERTY_OWNERSHIP_TYPE_MANDATORY, ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when added member property has invalid ownershipType")
    void validate_PropertyInvalidOwnershipType_ThrowsException() {
        UserProperty prop = UserProperty.builder()
                .localityType(FamilyConstants.LOCALITY_TYPE_URBAN)
                .ownershipType("INSTITUTION")
                .build();

        validRequest.getMember().setProperties(Collections.singletonList(prop));

        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);
        doThrow(new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_INVALID_OWNERSHIP_TYPE_PREFIX + "INSTITUTION"))
                .when(masterDataService).validateOwnershipType(any(), eq("pg.citya"), eq("INSTITUTION"));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
        assertEquals(FamilyConstants.MSG_INVALID_OWNERSHIP_TYPE_PREFIX + "INSTITUTION", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when added member has negative annual income")
    void validate_NegativeAnnualIncome_ThrowsException() {
        UserIncomeDetail income = UserIncomeDetail.builder()
                .annualIncome(BigDecimal.valueOf(-500))
                .build();

        validRequest.getMember().setIncome(income);

        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should throw exception when added member vehicle lacks vehicleNumber")
    void validate_VehicleMissingVehicleNumber_ThrowsException() {
        UserVehicleDetail vehicle = UserVehicleDetail.builder()
                .vehicleNumber("")
                .build();

        validRequest.getMember().setVehicles(Collections.singletonList(vehicle));

        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
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

        validRequest.getMember().setCitizenDetails(citizenDetails);

        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);
        when(fileStoreClient.validateFileExists("pg.citya", "missing-file-id")).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class, () -> validator.validate(validRequest, activeFamily));
        assertEquals(FamilyConstants.ERROR_FILESTORE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("Should successfully validate when assets are declared at root level of AddMemberRequest")
    void validate_RootLevelAssets_Success() {
        UserProperty rootProp = UserProperty.builder()
                .localityType(FamilyConstants.LOCALITY_TYPE_URBAN)
                .ownershipType("INDIVIDUAL")
                .consentToken("CONSENT-VERIFIED-12345")
                .urbanPropertyDetail(UrbanPropertyDetail.builder()
                        .upyogPropertyId("UPYOG-PROP-12345")
                        .consentToken("CONSENT-VERIFIED-12345")
                        .build())
                .build();

        UserIncomeDetail rootIncome = UserIncomeDetail.builder()
                .annualIncome(BigDecimal.valueOf(500000))
                .occupation("SALARIED")
                .build();

        UserVehicleDetail rootVehicle = UserVehicleDetail.builder()
                .vehicleNumber("CH01AB1234")
                .build();

        CitizenProfile rootCitizen = CitizenProfile.builder()
                .name("Root Citizen")
                .caste("OBC")
                .build();

        AddMemberRequest rootPayloadRequest = AddMemberRequest.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .userUuid("member-uuid-2")
                .relationshipWithHead("SPOUSE")
                .consentToken("CONSENT-VERIFIED-12345")
                .citizenDetails(rootCitizen)
                .properties(Collections.singletonList(rootProp))
                .income(rootIncome)
                .vehicles(Collections.singletonList(rootVehicle))
                .build();

        when(familyRepository.findActiveFamilyIdsByMemberUuid("member-uuid-2")).thenReturn(Collections.emptyList());
        when(requesterDxClient.verifyConsentToken(eq("CONSENT-VERIFIED-12345"), eq("member-uuid-2"), eq("FAM-2026-0001"), eq("pg.citya"), any())).thenReturn(true);
        when(userClient.validatePropertyConsentToken(eq("UPYOG-PROP-12345"), eq("CONSENT-VERIFIED-12345"), any())).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(rootPayloadRequest, activeFamily));
        assertNotNull(rootPayloadRequest.getMember());
        assertEquals("Root Citizen", rootPayloadRequest.getMember().getName());
        assertEquals(1, rootPayloadRequest.getMember().getProperties().size());
        assertEquals(rootIncome, rootPayloadRequest.getMember().getIncome());
        assertEquals(1, rootPayloadRequest.getMember().getVehicles().size());
    }
}

