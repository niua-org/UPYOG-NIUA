package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserIncomeRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.asset.IncomeRequest;
import org.egov.user.web.contract.asset.IncomeSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying business validations, security boundaries, and persistence workflows in {@link IncomeServiceImpl}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class IncomeServiceImplTest {

    private UserIncomeRepository incomeRepository;
    private UserRepository userRepository;
    private UserDocumentRepository documentRepository;
    private org.egov.user.persistence.repository.FamilyRestRepository familyRestRepository;
    private AssetAuthorizationService assetAuthorizationService;
    private org.egov.user.config.UserConfiguration userConfiguration;
    private IncomeServiceImpl incomeService;

    private static final String TEST_USER_UUID = "citizen-uuid-001";
    private static final String TEST_MEMBER_UUID = "member-uuid-002";
    private static final String TEST_INCOME_ID = "inc-001";
    private static final String TEST_TENANT_ID = "pg.citya";
    private static final String TEST_OPERATOR_UUID = "operator-uuid-555";
    private static final Long TEST_OPERATOR_ID = 50L;

    @BeforeEach
    public void setUp() {
        incomeRepository = mock(UserIncomeRepository.class);
        userRepository = mock(UserRepository.class);
        documentRepository = mock(UserDocumentRepository.class);
        familyRestRepository = mock(org.egov.user.persistence.repository.FamilyRestRepository.class);

        assetAuthorizationService = new AssetAuthorizationService(userRepository, familyRestRepository);

        userConfiguration = mock(org.egov.user.config.UserConfiguration.class);
        when(userConfiguration.getIncomeIdentifierTypeCacheTtlMillis()).thenReturn(300000L);
        when(userConfiguration.getMdmsIncomeIdentifierTypeEnabled()).thenReturn(false);
        when(userConfiguration.getAllowedIncomeIdentifierTypes()).thenReturn("PAN,TAN,GSTIN");

        incomeService = new IncomeServiceImpl(
                incomeRepository,
                userRepository,
                documentRepository,
                assetAuthorizationService,
                userConfiguration,
                null
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
    @DisplayName("Should create income declaration successfully when valid request is provided")
    public void testCreateIncome_Success() {
        User citizen = User.builder()
                .id(101L)
                .uuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();
        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(citizen));

        UserIncome mockPersisted = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("750000.00"))
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(incomeRepository.findIncomeById(any())).thenReturn(mockPersisted);

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("750000.00"))
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .build();

        UserIncome result = incomeService.createIncome(request);

        assertNotNull(result);
        assertEquals(TEST_INCOME_ID, result.getId());
        assertEquals("SALARIED", result.getOccupation());

        ArgumentCaptor<UserIncome> captor = ArgumentCaptor.forClass(UserIncome.class);
        verify(incomeRepository, times(1)).saveIncome(captor.capture());
        UserIncome saved = captor.getValue();
        assertEquals(TEST_USER_UUID, saved.getUserUuid());
        assertEquals(TEST_TENANT_ID, saved.getTenantId());
        assertEquals(TEST_OPERATOR_UUID, saved.getCreatedBy());
        assertEquals(UserConstants.STATUS_ACTIVE_UPPERCASE, saved.getStatus());
    }

    @Test
    @DisplayName("Should attach salary proof and tax proof documents during creation")
    public void testCreateIncome_WithDocuments_Success() {
        User citizen = User.builder()
                .id(101L)
                .uuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();
        when(userRepository.findAll(any(UserSearchCriteria.class)))
                .thenReturn(Collections.singletonList(citizen));

        UserDocument salaryDoc = UserDocument.builder()
                .filestoreId("fs-salary-123")
                .documentType("SALARY_SLIP")
                .build();

        UserDocument taxDoc = UserDocument.builder()
                .filestoreId("fs-tax-456")
                .documentType("FORM_16")
                .build();

        when(incomeRepository.findIncomeById(any())).thenReturn(UserIncome.builder().id(TEST_INCOME_ID).build());

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("900000.00"))
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .salaryProofDocument(salaryDoc)
                .taxProofDocument(taxDoc)
                .build();

        incomeService.createIncome(request);

        verify(documentRepository, times(2)).create(any(UserDocument.class));
        verify(incomeRepository, times(1)).saveIncome(any(UserIncome.class));
    }

    @Test
    @DisplayName("Should throw CustomException when creation request is null")
    public void testCreateIncome_ThrowsException_WhenRequestNull() {
        CustomException ex = assertThrows(CustomException.class, () -> incomeService.createIncome(null));
        assertEquals(UserConstants.ERROR_INVALID_INCOME_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when userUuid is missing")
    public void testCreateIncome_ThrowsException_WhenUserUuidMissing() {
        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PAN")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.createIncome(request));
        assertEquals(UserConstants.ERROR_USER_UUID_REQUIRED, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when citizen not found in eg_user")
    public void testCreateIncome_ThrowsException_WhenCitizenNotFound() {
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PAN")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.createIncome(request));
        assertEquals(UserConstants.ERROR_USER_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when tenantId is missing")
    public void testCreateIncome_ThrowsException_WhenTenantIdMissing() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PAN")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.createIncome(request));
        assertEquals("INVALID_TENANT_ID", ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when occupation or identifierType is missing")
    public void testCreateIncome_ThrowsException_WhenMandatoryFieldMissing() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PAN")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.createIncome(request));
        assertEquals(UserConstants.ERROR_INCOME_MANDATORY_FIELDS, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when annual income is negative")
    public void testCreateIncome_ThrowsException_WhenAnnualIncomeNegative() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("-100.00"))
                .identifierType("PAN")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.createIncome(request));
        assertEquals(UserConstants.ERROR_INVALID_INCOME_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when identifierType is not in PAN/TAN/GSTIN")
    public void testCreateIncome_ThrowsException_WhenIdentifierTypeInvalid() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PASSPORT")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.createIncome(request));
        assertEquals(UserConstants.ERROR_INVALID_INCOME_REQUEST, ex.getCode());
    }

    @Test
    @DisplayName("Should update existing income declaration successfully")
    public void testUpdateIncome_Success() {
        UserIncome existing = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(incomeRepository.findIncomeById(TEST_INCOME_ID)).thenReturn(existing);

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .incomeId(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .annualIncome(new BigDecimal("700000.00"))
                .occupation("BUSINESS")
                .build();

        UserIncome result = incomeService.updateIncome(request);

        assertNotNull(result);
        ArgumentCaptor<UserIncome> captor = ArgumentCaptor.forClass(UserIncome.class);
        verify(incomeRepository, times(1)).saveIncome(captor.capture());
        UserIncome saved = captor.getValue();
        assertEquals(new BigDecimal("700000.00"), saved.getAnnualIncome());
        assertEquals("BUSINESS", saved.getOccupation());
        assertEquals(TEST_OPERATOR_UUID, saved.getLastModifiedBy());
    }

    @Test
    @DisplayName("Should throw CustomException on update when income not found")
    public void testUpdateIncome_ThrowsException_WhenIncomeNotFound() {
        when(incomeRepository.findIncomeById(TEST_INCOME_ID)).thenReturn(null);

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .incomeId(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.updateIncome(request));
        assertEquals(UserConstants.ERROR_INCOME_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException on update when ownership mismatch occurs")
    public void testUpdateIncome_ThrowsException_WhenOwnershipMismatch() {
        UserIncome existing = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid("other-citizen-999")
                .build();
        when(incomeRepository.findIncomeById(TEST_INCOME_ID)).thenReturn(existing);

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .incomeId(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.updateIncome(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
        verify(incomeRepository, never()).saveIncome(any());
    }

    @Test
    @DisplayName("Should search income declarations when valid criteria provided")
    public void testSearchIncome_Success() {
        UserIncome found = UserIncome.builder().id(TEST_INCOME_ID).build();
        when(incomeRepository.searchIncome(any(IncomeSearchCriteria.class))).thenReturn(Collections.singletonList(found));

        IncomeSearchCriteria criteria = IncomeSearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .build();

        List<UserIncome> list = incomeService.searchIncome(criteria, createTestRequestInfo());
        assertEquals(1, list.size());
        assertEquals(TEST_INCOME_ID, list.get(0).getId());
    }

    @Test
    @DisplayName("Should throw CustomException on search when criteria has no filters")
    public void testSearchIncome_ThrowsException_WhenCriteriaEmpty() {
        IncomeSearchCriteria emptyCriteria = IncomeSearchCriteria.builder().build();
        CustomException ex = assertThrows(CustomException.class, () -> incomeService.searchIncome(emptyCriteria, createTestRequestInfo()));
        assertEquals(UserConstants.ERROR_INVALID_SEARCH_CRITERIA, ex.getCode());
    }

    @Test
    @DisplayName("Should soft-delete income declaration successfully")
    public void testDeleteIncome_Success() {
        UserIncome existing = UserIncome.builder()
                .id(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();
        when(incomeRepository.findIncomeById(TEST_INCOME_ID)).thenReturn(existing);
        when(incomeRepository.updateIncomeStatus(eq(TEST_INCOME_ID), eq(TEST_USER_UUID), eq(UserConstants.STATUS_INACTIVE_UPPERCASE), eq(TEST_OPERATOR_UUID))).thenReturn(true);

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createTestRequestInfo())
                .incomeId(TEST_INCOME_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        UserIncome result = incomeService.deleteIncome(request);

        assertNotNull(result);
        verify(incomeRepository, times(1)).updateIncomeStatus(eq(TEST_INCOME_ID), eq(TEST_USER_UUID), eq(UserConstants.STATUS_INACTIVE_UPPERCASE), eq(TEST_OPERATOR_UUID));
    }

    @Test
    @DisplayName("Should permit citizen to create own income declaration")
    public void testCreateIncome_CitizenSelfAccess_Success() {
        User citizen = User.builder().id(101L).uuid(TEST_USER_UUID).tenantId(TEST_TENANT_ID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(citizen));

        UserIncome mockPersisted = UserIncome.builder()
                .id(TEST_INCOME_ID).userUuid(TEST_USER_UUID).tenantId(TEST_TENANT_ID)
                .occupation("BUSINESS").annualIncome(new BigDecimal("1200000.00"))
                .identifierType("GSTIN").identifierValue("07AAAAA0000A1Z5")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE).build();
        when(incomeRepository.findIncomeById(any())).thenReturn(mockPersisted);

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createCitizenRequestInfo(TEST_USER_UUID))
                .userUuid(TEST_USER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("BUSINESS")
                .annualIncome(new BigDecimal("1200000.00"))
                .identifierType("GSTIN")
                .identifierValue("07AAAAA0000A1Z5")
                .build();

        UserIncome result = incomeService.createIncome(request);
        assertNotNull(result);
        assertEquals(TEST_USER_UUID, result.getUserUuid());
    }

    @Test
    @DisplayName("Should permit Head of Family to create income declaration for household member")
    public void testCreateIncome_HeadOfFamily_Success() {
        User member = User.builder().id(102L).uuid(TEST_MEMBER_UUID).tenantId(TEST_TENANT_ID).build();
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(member));
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(TEST_USER_UUID), eq(TEST_MEMBER_UUID), eq(TEST_TENANT_ID), any()))
                .thenReturn(true);

        UserIncome mockPersisted = UserIncome.builder()
                .id(TEST_INCOME_ID).userUuid(TEST_MEMBER_UUID).tenantId(TEST_TENANT_ID)
                .occupation("STUDENT").annualIncome(BigDecimal.ZERO)
                .identifierType("PAN").identifierValue("ABCDE1234F")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE).build();
        when(incomeRepository.findIncomeById(any())).thenReturn(mockPersisted);

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createCitizenRequestInfo(TEST_USER_UUID))
                .userUuid(TEST_MEMBER_UUID)
                .tenantId(TEST_TENANT_ID)
                .occupation("STUDENT")
                .annualIncome(BigDecimal.ZERO)
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .build();

        UserIncome result = incomeService.createIncome(request);
        assertNotNull(result);
        assertEquals(TEST_MEMBER_UUID, result.getUserUuid());
    }

    @Test
    @DisplayName("Should reject income creation with UNAUTHORIZED_ACCESS when citizen caller is NOT Head of Family")
    public void testCreateIncome_UnauthorizedCitizen_ThrowsException() {
        when(familyRestRepository.isCallerHeadOfFamilyForMember(eq(TEST_USER_UUID), eq("stranger-uuid"), eq(TEST_TENANT_ID), any()))
                .thenReturn(false);

        IncomeRequest request = IncomeRequest.builder()
                .requestInfo(createCitizenRequestInfo(TEST_USER_UUID))
                .userUuid("stranger-uuid")
                .tenantId(TEST_TENANT_ID)
                .occupation("SALARIED")
                .annualIncome(new BigDecimal("500000.00"))
                .identifierType("PAN")
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> incomeService.createIncome(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_ACCESS, ex.getCode());
    }

    @Test
    @DisplayName("Should auto-scope unconstrained citizen income search to caller UUID")
    public void testSearchIncome_CitizenSelfSearch_AutoScoped() {
        IncomeSearchCriteria criteria = IncomeSearchCriteria.builder()
                .tenantId(TEST_TENANT_ID)
                .build();

        when(incomeRepository.searchIncome(any(IncomeSearchCriteria.class)))
                .thenAnswer(invocation -> {
                    IncomeSearchCriteria passed = invocation.getArgument(0);
                    assertEquals(TEST_USER_UUID, passed.getUserUuid());
                    return Collections.singletonList(UserIncome.builder().id(TEST_INCOME_ID).userUuid(TEST_USER_UUID).build());
                });

        List<UserIncome> results = incomeService.searchIncome(criteria, createCitizenRequestInfo(TEST_USER_UUID));
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Should fetch and cache identifier types from MDMS when enabled")
    public void testGetAllowedIdentifierTypes_FromMdms_Success() {
        org.springframework.web.client.RestTemplate mockRestTemplate = mock(org.springframework.web.client.RestTemplate.class);
        org.egov.user.config.UserConfiguration config = org.egov.user.config.UserConfiguration.builder()
                .mdmsHost("http://egov-mdms-service:8080/")
                .mdmsPath("egov-mdms-service/v1/_search")
                .mdmsIncomeIdentifierTypeEnabled(true)
                .mdmsIncomeIdentifierTypeModuleName("common-masters")
                .mdmsIncomeIdentifierTypeMasterName("IncomeIdentifier")
                .mdmsIncomeIdentifierTypeFilter("[?(@.active==true)]")
                .allowedIncomeIdentifierTypes("PAN,TAN,GSTIN")
                .incomeIdentifierTypeCacheTtlMillis(300000L)
                .build();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = "{\"MdmsRes\":{\"common-masters\":{\"IncomeIdentifier\":[{\"code\":\"PAN\"},{\"code\":\"TAN\"},{\"code\":\"GSTIN\"},{\"code\":\"CUSTOM_TAX\"}]}}}";
        try {
            com.fasterxml.jackson.databind.JsonNode mockNode = mapper.readTree(json);
            when(mockRestTemplate.postForObject(any(String.class), any(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                    .thenReturn(mockNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        IncomeServiceImpl mdmsService = new IncomeServiceImpl(
                incomeRepository, userRepository, documentRepository, assetAuthorizationService, config, mockRestTemplate
        );

        java.util.Set<String> types = mdmsService.getAllowedIdentifierTypes(createTestRequestInfo(), "pg.citya");
        assertNotNull(types);
        assertEquals(4, types.size());
        assertTrue(types.contains("CUSTOM_TAX"));
        assertTrue(types.contains("PAN"));

        // Second call should hit the cache and not invoke restTemplate again
        java.util.Set<String> cachedTypes = mdmsService.getAllowedIdentifierTypes(createTestRequestInfo(), "pg.citya");
        assertEquals(4, cachedTypes.size());
        verify(mockRestTemplate, times(1)).postForObject(any(String.class), any(), eq(com.fasterxml.jackson.databind.JsonNode.class));
    }

    @Test
    @DisplayName("Should fallback to configured properties when MDMS fails")
    public void testGetAllowedIdentifierTypes_MdmsFailure_FallbackToProperties() {
        org.springframework.web.client.RestTemplate mockRestTemplate = mock(org.springframework.web.client.RestTemplate.class);
        org.egov.user.config.UserConfiguration config = org.egov.user.config.UserConfiguration.builder()
                .mdmsHost("http://egov-mdms-service:8080/")
                .mdmsPath("egov-mdms-service/v1/_search")
                .mdmsIncomeIdentifierTypeEnabled(true)
                .mdmsIncomeIdentifierTypeModuleName("common-masters")
                .mdmsIncomeIdentifierTypeMasterName("IdentifierType")
                .mdmsIncomeIdentifierTypeFilter("[?(@.active==true)]")
                .allowedIncomeIdentifierTypes("PAN,TAN,GSTIN,AADHAAR_TAX")
                .incomeIdentifierTypeCacheTtlMillis(300000L)
                .build();

        when(mockRestTemplate.postForObject(any(String.class), any(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenThrow(new org.springframework.web.client.RestClientException("Connection refused"));

        IncomeServiceImpl mdmsService = new IncomeServiceImpl(
                incomeRepository, userRepository, documentRepository, assetAuthorizationService, config, mockRestTemplate
        );

        java.util.Set<String> types = mdmsService.getAllowedIdentifierTypes(createTestRequestInfo(), "pg.citya");
        assertNotNull(types);
        assertEquals(4, types.size());
        assertTrue(types.contains("AADHAAR_TAX"));
    }
}
