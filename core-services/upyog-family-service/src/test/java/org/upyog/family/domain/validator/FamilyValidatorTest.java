package org.upyog.family.domain.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.domain.service.master.FamilyMasterDataService;
import org.upyog.family.persistence.repository.FamilyRepository;
import org.upyog.family.persistence.repository.FileStoreClient;
import org.upyog.family.persistence.repository.UserClient;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyDocument;
import org.upyog.family.web.contract.FamilyMember;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySplitRequest;
import org.upyog.family.web.contract.RuralPropertyDetail;
import org.upyog.family.web.contract.UserIncomeDetail;
import org.upyog.family.web.contract.UserProperty;
import org.upyog.family.web.contract.UserVehicleDetail;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyValidatorTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FileStoreClient fileStoreClient;

    @Mock
    private FamilyMasterDataService familyMasterDataService;

    @Mock
    private UserClient userClient;

    @Mock
    private FamilyConfiguration configuration;

    @InjectMocks
    private FamilyValidator validator;

    private Family family;
    private FamilyRequest request;

    @BeforeEach
    void setUp() {
        FamilyMember head = FamilyMember.builder()
                .userUuid("user-uuid-head")
                .relationshipWithHead("HEAD")
                .isHead(true)
                .active(true)
                .build();

        FamilyMember member = FamilyMember.builder()
                .userUuid("user-uuid-spouse")
                .relationshipWithHead("SPOUSE")
                .isHead(false)
                .active(true)
                .build();

        family = Family.builder()
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-head")
                .status(FamilyStatus.ACTIVE)
                .members(new ArrayList<>(Arrays.asList(head, member)))
                .build();

        request = FamilyRequest.builder()
                .family(family)
                .build();
    }

    @Test
    @DisplayName("validateCreate should succeed for valid family unit")
    void validateCreate_Success() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validator.validateCreate(request));
    }

    @Test
    @DisplayName("validateCreate should succeed when caller is different from Head of Family (e.g. Son creating family with Father as HOF)")
    void validateCreate_CallerNotHead_Success() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("caller-uuid-son").build())
                .build();
        request.setRequestInfo(requestInfo);

        assertDoesNotThrow(() -> validator.validateCreate(request));
        assertEquals("user-uuid-head", family.getHeadUserUuid());
    }

    @Test
    @DisplayName("validateCreate should fail when relationship is invalid according to MDMS")
    void validateCreate_InvalidRelationship() {
        doThrow(new CustomException(FamilyConstants.ERROR_INVALID_RELATIONSHIP, "Invalid relationship"))
                .when(familyMasterDataService).validateRelationship(any(), eq("pg.citya"), eq("HEAD"));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_INVALID_RELATIONSHIP, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when tenantId is blank")
    void validateCreate_MissingTenantId() {
        family.setTenantId("");

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when no Head of Family exists")
    void validateCreate_NoHead() {
        family.getMembers().forEach(m -> m.setIsHead(false));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when multiple Heads of Family exist")
    void validateCreate_MultipleHeads() {
        family.getMembers().forEach(m -> m.setIsHead(true));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_HEAD_OF_FAMILY_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when headUserUuid mismatches isHead member UUID")
    void validateCreate_HeadUuidMismatch() {
        family.setHeadUserUuid("different-uuid");

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_HEAD_UUID_MISMATCH, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when member is already active in another family")
    void validateCreate_MemberAlreadyInAnotherFamily() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid("user-uuid-head"))
                .thenReturn(Collections.singletonList("FAM-EXISTING-001"));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_MEMBER_ALREADY_EXISTS, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when document filestoreId does not exist")
    void validateCreate_DocumentNotFound() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());
        when(fileStoreClient.validateFileExists("pg.citya", "invalid-store-id")).thenReturn(false);

        FamilyDocument doc = FamilyDocument.builder()
                .documentType("ADDRESS_PROOF")
                .filestoreId("invalid-store-id")
                .build();
        family.setDocuments(Collections.singletonList(doc));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_FILESTORE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("validateSplit should succeed when splitting subset of active members")
    void validateSplit_Success() {
        FamilySplitRequest splitRequest = FamilySplitRequest.builder()
                .tenantId("pg.citya")
                .parentFamilyId("FAM-PARENT-001")
                .newFamilyHeadUserUuid("user-uuid-spouse")
                .splittingMemberUserUuids(Collections.singletonList("user-uuid-spouse"))
                .build();

        assertDoesNotThrow(() -> validator.validateSplit(splitRequest, family));
    }

    @Test
    @DisplayName("validateSplit should fail when trying to split all members")
    void validateSplit_CannotSplitAll() {
        FamilySplitRequest splitRequest = FamilySplitRequest.builder()
                .tenantId("pg.citya")
                .parentFamilyId("FAM-PARENT-001")
                .newFamilyHeadUserUuid("user-uuid-head")
                .splittingMemberUserUuids(Arrays.asList("user-uuid-head", "user-uuid-spouse"))
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateSplit(splitRequest, family));
        assertEquals(FamilyConstants.ERROR_INVALID_SPLIT, ex.getCode());
    }

    @Test
    @DisplayName("validateMerge should succeed for two distinct active families")
    void validateMerge_Success() {
        Family mergingFamily = Family.builder()
                .familyId("FAM-MERGE-002")
                .tenantId("pg.citya")
                .headUserUuid("user-uuid-merge-head")
                .status(FamilyStatus.ACTIVE)
                .members(Collections.singletonList(FamilyMember.builder()
                        .userUuid("user-uuid-merge-head")
                        .active(true)
                        .isHead(true)
                        .build()))
                .build();
        family.setFamilyId("FAM-SURVIVING-001");

        FamilyMergeRequest mergeRequest = FamilyMergeRequest.builder()
                .tenantId("pg.citya")
                .survivingFamilyId("FAM-SURVIVING-001")
                .mergingFamilyId("FAM-MERGE-002")
                .commonHeadUserUuid("user-uuid-head")
                .build();

        assertDoesNotThrow(() -> validator.validateMerge(mergeRequest, family, mergingFamily));
    }

    @Test
    @DisplayName("validateCreate should fail when any member's identity is not verified via Aadhaar/e-KYC")
    void validateCreate_MemberIdentityNotVerified() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());
        lenient().doThrow(new CustomException(FamilyConstants.ERROR_USER_NOT_VERIFIED, FamilyConstants.MSG_USER_NOT_VERIFIED))
                .when(userClient).validateUserIsVerified(eq("user-uuid-spouse"), any());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_USER_NOT_VERIFIED, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should fail when rural property detail is missing for RURAL locality")
    void validateCreate_RuralPropertyMissingDetail() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());

        UserProperty prop = UserProperty.builder()
                .localityType("RURAL")
                .ruralPropertyDetail(null)
                .build();

        family.getMembers().get(0).setProperties(Collections.singletonList(prop));

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("validateCreate should succeed with rich composite payload (Rural/Urban property, Income, Vehicles)")
    void validateCreate_FullCompositePayload_Success() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());

        UserProperty ruralProp = UserProperty.builder()
                .localityType("RURAL")
                .ownershipType("JOINT")
                .ruralPropertyDetail(RuralPropertyDetail.builder()
                        .gramPanchayat("Rampur GP")
                        .villageName("Rampur Khas")
                        .surveyNumber("KHASRA-842/1")
                        .build())
                .build();

        UserIncomeDetail income = UserIncomeDetail.builder()
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("850000.00"))
                .identifierType("PAN")
                .identifierValue("AFZPK7190K")
                .build();

        UserVehicleDetail vehicle = UserVehicleDetail.builder()
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .build();

        family.getMembers().get(0).setProperties(Collections.singletonList(ruralProp));
        family.getMembers().get(0).setIncome(income);
        family.getMembers().get(0).setVehicles(Collections.singletonList(vehicle));
        family.setFamilyAddressPropertyId("KHASRA-842/1");

        assertDoesNotThrow(() -> validator.validateCreate(request));
    }

    /**
     * Fix for PR Comment #15 (FamilyValidator.java:210):
     * Verifies that validateCreate throws an exception when familyAddressPropertyId does not match
     * any of the declared properties of the family members.
     */
    @Test
    @DisplayName("validateCreate should fail when familyAddressPropertyId does not match declared properties")
    void validateCreate_InvalidResidencePropertyId_ThrowsException() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());

        UserProperty ruralProp = UserProperty.builder()
                .localityType("RURAL")
                .ruralPropertyDetail(RuralPropertyDetail.builder()
                        .gramPanchayat("Rampur GP")
                        .villageName("Rampur Khas")
                        .surveyNumber("KHASRA-842/1")
                        .build())
                .build();

        family.getMembers().get(0).setProperties(Collections.singletonList(ruralProp));
        family.setFamilyAddressPropertyId("UNMATCHED-PROPERTY-999");

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
        assertEquals(FamilyConstants.MSG_INVALID_RESIDENCE_PROPERTY_PREFIX + "UNMATCHED-PROPERTY-999"
                + FamilyConstants.MSG_INVALID_RESIDENCE_PROPERTY_SUFFIX, ex.getMessage());
    }

    /**
     * Fix for PR Comment #15 (FamilyValidator.java:210):
     * Verifies that validateCreate succeeds when familyAddressPropertyId is provided
     * but no member properties are declared (e.g. rented / external residence).
     */
    @Test
    @DisplayName("validateCreate should succeed with residence property ID when no member properties declared")
    void validateCreate_ResidencePropertyIdWithoutDeclaredProperties_Success() {
        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());

        family.setFamilyAddressPropertyId("RENTED-MUNICIPAL-PROP-101");

        assertDoesNotThrow(() -> validator.validateCreate(request));
    }

    @Test
    @DisplayName("validateMerge should fail when merging family into itself")
    void validateMerge_SelfMergeFails() {
        family.setFamilyId("FAM-001");
        FamilyMergeRequest mergeRequest = FamilyMergeRequest.builder()
                .tenantId("pg.citya")
                .survivingFamilyId("FAM-001")
                .mergingFamilyId("FAM-001")
                .commonHeadUserUuid("user-uuid-head")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateMerge(mergeRequest, family, family));
        assertEquals(FamilyConstants.ERROR_INVALID_MERGE, ex.getCode());
    }

    /**
     * Fix for PR Comment #12 (FamilyServiceImpl.java:394):
     * Verifies that validateUpdate auto-normalizes legacy HoF relationship 'HEAD' to 'SELF' before MDMS validation.
     */
    @Test
    @DisplayName("validateUpdate should normalize legacy HEAD relationship to SELF for Head of Family")
    void validateUpdate_NormalizesHeadToSelf() {
        family.setFamilyId("FAM-001");
        FamilyMember head = family.getMembers().get(0);
        head.setRelationshipWithHead("HEAD");
        head.setIsHead(true);

        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.singletonList("FAM-001"));

        assertDoesNotThrow(() -> validator.validateUpdate(request, family));

        // Verify HoF relationship is normalized to SELF
        assertEquals(FamilyConstants.RELATIONSHIP_SELF, head.getRelationshipWithHead());
        verify(familyMasterDataService).validateRelationship(any(), eq("pg.citya"), eq(FamilyConstants.RELATIONSHIP_SELF));
    }

    /**
     * Fix for PR Comment #14 (FamilyValidator.java:180):
     * Verifies that minors (< 18 yrs or CHILD) are exempt from individual e-KYC verification
     * when memberVerificationMandatoryForAll is false.
     */
    @Test
    @DisplayName("validateCreate should exempt minor child from individual verification")
    void validateCreate_MinorChild_ExemptFromVerification() {
        FamilyMember child = FamilyMember.builder()
                .userUuid("user-uuid-child")
                .relationshipWithHead("CHILD")
                .isHead(false)
                .active(true)
                .dob(System.currentTimeMillis() - (10L * 365L * 24L * 60L * 60L * 1000L))
                .build();
        family.getMembers().add(child);

        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());
        when(configuration.getMemberVerificationMandatoryForAll()).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateCreate(request));

        // Head and adult spouse are verified, but minor child is exempt
        verify(userClient).validateUserIsVerified(eq("user-uuid-head"), any());
        verify(userClient, never()).validateUserIsVerified(eq("user-uuid-child"), any());
    }

    /**
     * Fix for PR Comment #14 (FamilyValidator.java:180):
     * Verifies that dependents carrying HoF consent tokens are exempt from individual verification.
     */
    @Test
    @DisplayName("validateCreate should exempt dependent with HoF consent token from individual verification")
    void validateCreate_DependentWithConsent_ExemptFromVerification() {
        FamilyMember dependent = FamilyMember.builder()
                .userUuid("user-uuid-dependent")
                .relationshipWithHead("DEPENDENT")
                .isHead(false)
                .active(true)
                .consentToken("HOF-CONSENT-TOKEN-12345")
                .build();
        family.getMembers().add(dependent);

        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());
        when(configuration.getMemberVerificationMandatoryForAll()).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateCreate(request));

        // Dependent with consent token is exempt
        verify(userClient, never()).validateUserIsVerified(eq("user-uuid-dependent"), any());
    }

    /**
     * Fix for PR Comment #14 (FamilyValidator.java:180):
     * Verifies that when memberVerificationMandatoryForAll is true, even minors must be verified.
     */
    @Test
    @DisplayName("validateCreate should require verification for minor when memberVerificationMandatoryForAll is true")
    void validateCreate_MinorRequiresVerification_WhenMandatoryForAll() {
        FamilyMember child = FamilyMember.builder()
                .userUuid("user-uuid-child")
                .relationshipWithHead("CHILD")
                .isHead(false)
                .active(true)
                .dob(System.currentTimeMillis() - (10L * 365L * 24L * 60L * 60L * 1000L))
                .build();
        family.getMembers().add(child);

        when(familyRepository.findActiveFamilyIdsByMemberUuid(anyString())).thenReturn(Collections.emptyList());
        when(configuration.getMemberVerificationMandatoryForAll()).thenReturn(true);
        lenient().doThrow(new CustomException("USER_NOT_VERIFIED", "User is not verified"))
                .when(userClient).validateUserIsVerified(eq("user-uuid-child"), any());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateCreate(request));
        assertEquals("USER_NOT_VERIFIED", ex.getCode());
    }
}
