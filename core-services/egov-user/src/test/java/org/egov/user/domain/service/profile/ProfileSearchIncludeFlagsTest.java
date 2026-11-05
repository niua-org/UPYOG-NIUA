package org.egov.user.domain.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.egov.user.domain.service.address.AddressService;
import org.egov.user.domain.service.document.UserDocumentService;
import org.egov.user.domain.service.identity.IdentityVerificationService;
import org.egov.user.domain.service.profile.validator.ProfileValidator;
import org.egov.user.domain.service.profile.validator.ProfileValidatorImpl;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.UserSearchResponseContent;
import org.egov.user.web.contract.profile.ProfileIncludeEntity;
import org.egov.user.web.contract.profile.ProfileResponse;
import org.egov.user.web.contract.profile.ProfileSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite verifying modular entity inclusion flags ({@code include: [PROFILE, ADDRESS, DOCUMENT]})
 * for citizen profile search ({@code /profile/_search}).
 * <p>
 * Covers:
 * <ul>
 *   <li>Default inclusion semantics (defaults to {@link ProfileIncludeEntity#PROFILE} when omitted)</li>
 *   <li>Selective enrichment of {@code ADDRESS} and {@code DOCUMENT} collections</li>
 *   <li>Exclusion of addresses and documents from legacy constructor to prevent PII exposure for existing callers</li>
 *   <li>Rejection of unknown include entity sections (e.g., "FOO", "INVALID_SECTION") with HTTP 400 {@link CustomException}</li>
 *   <li>Authorization restriction preventing citizen callers from searching ADDRESS/DOCUMENT for other citizens' UUIDs</li>
 *   <li>Strict JSON property deserialization via {@code include}</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProfileSearchIncludeFlagsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileValidator profileValidator;

    @Mock
    private IdentityVerificationService identityVerificationService;

    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserDocumentRepository userDocumentRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private UserDocumentService userDocumentService;

    @Mock
    private org.egov.user.persistence.repository.FamilyRestRepository familyRestRepository;

    private ProfileServiceImpl profileService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        profileService = new ProfileServiceImpl(
                userRepository,
                profileValidator,
                identityVerificationService,
                encryptionDecryptionUtil,
                addressRepository,
                userDocumentRepository,
                null,
                addressService,
                userDocumentService,
                null,
                familyRestRepository
        );
        objectMapper = new ObjectMapper();
        when(encryptionDecryptionUtil.encryptObject(any(), any(), any())).thenAnswer(i -> i.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(), any(), any(), any())).thenAnswer(i -> i.getArgument(0));
    }

    private User createSampleUser(String uuid) {
        return User.builder()
                .id(101L)
                .uuid(uuid)
                .username("9876543210")
                .name("Rohan Sharma")
                .mobileNumber("9876543210")
                .tenantId("pg")
                .fatherName("Suresh Sharma")
                .motherName("Sunita Sharma")
                .gender(org.egov.user.domain.model.enums.Gender.MALE)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should default to PROFILE when include flag is null")
    public void testSearchProfileDefaultInclude() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        User result = results.get(0);
        assertEquals("Rohan Sharma", result.getName());
        assertNull(result.getAddresses());
        assertNull(result.getDocuments());

        verify(addressService, never()).searchAddress(any(), any());
        verify(userDocumentService, never()).searchDocuments(any(), any());
    }

    @Test
    @DisplayName("Should enrich ADDRESS and DOCUMENT when both flags are specified")
    public void testSearchProfileWithAddressAndDocument() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS, ProfileIncludeEntity.DOCUMENT))
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));

        Address address = Address.builder().id(201L).city("City A").build();
        when(addressService.searchAddress(any(), any())).thenReturn(Collections.singletonList(address));

        UserDocument doc = UserDocument.builder().id("doc-1").documentType("IDENTITY_PROOF").build();
        when(userDocumentService.searchDocuments(any(), any())).thenReturn(Collections.singletonList(doc));

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        User resultUser = results.get(0);
        assertNotNull(resultUser.getAddresses());
        assertEquals(1, resultUser.getAddresses().size());
        assertNotNull(resultUser.getDocuments());
        assertEquals(1, resultUser.getDocuments().size());

        verify(addressService, times(1)).searchAddress(any(), any());
        verify(userDocumentService, times(1)).searchDocuments(any(), any());
    }

    @Test
    @DisplayName("Should omit demographics if only ADDRESS is requested in UserSearchResponseContent")
    public void testResponseContentWithOnlyAddress() throws Exception {
        User user = createSampleUser("uuid-123");
        Address address = Address.builder().id(201L).city("City A").build();
        user.setAddresses(Collections.singleton(address));

        Set<ProfileIncludeEntity> includes = Collections.singleton(ProfileIncludeEntity.ADDRESS);
        UserSearchResponseContent content = new UserSearchResponseContent(user, includes);

        assertEquals("uuid-123", content.getUuid());
        assertEquals(101L, content.getId());
        assertEquals("9876543210", content.getUserName());
        // Demographic fields should be null when PROFILE is not in include
        assertNull(content.getName());
        assertNull(content.getMobileNumber());
        assertNull(content.getFatherName());

        assertNotNull(content.getAddresses());
        assertEquals(1, content.getAddresses().size());
        assertNull(content.getDocuments());

        // Verify JSON serialization: addresses included, documents omitted when null
        String json = objectMapper.writeValueAsString(content);
        assertFalse(json.contains("\"documents\""));
        assertTrue(json.contains("\"addresses\""));
        assertTrue(json.contains("\"userName\":\"9876543210\""));
    }

    @Test
    @DisplayName("Should omit addresses and documents from JSON when null using Jackson serialization")
    public void testJacksonSerializationOmitsNullAddressesAndDocuments() throws Exception {
        User user = createSampleUser("uuid-123");
        UserSearchResponseContent content = new UserSearchResponseContent(user, Collections.singleton(ProfileIncludeEntity.PROFILE));

        ProfileResponse response = ProfileResponse.builder()
                .user(Collections.singletonList(content))
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"userName\":\"9876543210\""));
        assertTrue(json.contains("\"name\":\"Rohan Sharma\""));
        assertFalse(json.contains("\"addresses\""));
        assertFalse(json.contains("\"documents\""));
    }

    @Test
    @DisplayName("Should serialize user in ProfileResponse as a JSON List of users")
    public void testProfileResponseSerializesUserAsList() throws Exception {
        User user = createSampleUser("uuid-single-obj");
        UserSearchResponseContent content = new UserSearchResponseContent(user, Collections.singleton(ProfileIncludeEntity.PROFILE));

        ProfileResponse response = ProfileResponse.builder()
                .user(Collections.singletonList(content))
                .build();

        assertNotNull(response.getUser());
        assertEquals("uuid-single-obj", response.getUser().get(0).getUuid());

        String json = objectMapper.writeValueAsString(response);
        // Ensure "user" is serialized as a list/array [{...}]
        assertTrue(json.contains("\"user\":["), "Response JSON must format 'user' as a list/array");
    }

    @Test
    @DisplayName("Should deserialize ProfileSearchRequest with string uuid and reject array uuid")
    public void testProfileSearchRequestDeserialization_StringAndSingleListUuid() throws Exception {
        String jsonStringUuid = "{\"tenantId\":\"pg.citya\",\"uuid\":\"uuid-plain-string\"}";
        ProfileSearchRequest req1 = objectMapper.readValue(jsonStringUuid, ProfileSearchRequest.class);
        assertEquals("uuid-plain-string", req1.getUuid());
        assertFalse(req1.isMultipleUuidsProvided());

        String jsonArrayUuid = "{\"tenantId\":\"pg.citya\",\"uuid\":[\"uuid-single-in-array\"]}";
        ProfileSearchRequest req2 = objectMapper.readValue(jsonArrayUuid, ProfileSearchRequest.class);
        assertTrue(req2.isMultipleUuidsProvided());

        ProfileValidatorImpl validator = new ProfileValidatorImpl(null);
        CustomException ex = assertThrows(CustomException.class, () -> validator.validateSearch(req2));
        assertEquals(UserConstants.ERROR_INVALID_SEARCH_CRITERIA, ex.getCode());
        assertEquals(UserConstants.MSG_MULTIPLE_USERS_SEARCH_NOT_SUPPORTED, ex.getMessage());
    }

    @Test
    @DisplayName("Should reject search when JSON contains multiple UUIDs in uuid array")
    public void testProfileSearchRequestDeserialization_MultipleUuidsFailsValidation() throws Exception {
        String jsonMultipleUuids = "{\"tenantId\":\"pg.citya\",\"uuid\":[\"uuid-1\",\"uuid-2\"]}";
        ProfileSearchRequest req = objectMapper.readValue(jsonMultipleUuids, ProfileSearchRequest.class);
        assertTrue(req.isMultipleUuidsProvided());

        ProfileValidatorImpl validator = new ProfileValidatorImpl(null);
        CustomException ex = assertThrows(CustomException.class, () -> validator.validateSearch(req));
        assertEquals(UserConstants.ERROR_INVALID_SEARCH_CRITERIA, ex.getCode());
        assertEquals(UserConstants.MSG_MULTIPLE_USERS_SEARCH_NOT_SUPPORTED, ex.getMessage());
    }

    @Test
    @DisplayName("Should reject search when JSON contains userIDs or userIds")
    public void testProfileSearchRequestDeserialization_UserIdsFailsValidation() throws Exception {
        String jsonUserIds = "{\"tenantId\":\"pg.citya\",\"userIDs\":[101]}";
        ProfileSearchRequest req = objectMapper.readValue(jsonUserIds, ProfileSearchRequest.class);
        assertTrue(req.hasUserIdCriteria());

        ProfileValidatorImpl validator = new ProfileValidatorImpl(null);
        CustomException ex = assertThrows(CustomException.class, () -> validator.validateSearch(req));
        assertEquals(UserConstants.ERROR_INVALID_SEARCH_CRITERIA, ex.getCode());
        assertEquals(UserConstants.MSG_USER_ID_SEARCH_NOT_SUPPORTED, ex.getMessage());
    }

    @Test
    @DisplayName("Should support 'include' when deserializing ProfileSearchRequest")
    public void testDeserializationWithInclude() throws Exception {
        String json = "{\n" +
                "  \"tenantId\": \"pg.citya\",\n" +
                "  \"uuid\": [\"uuid-123\"],\n" +
                "  \"include\": [\"PROFILE\", \"ADDRESS\"]\n" +
                "}";

        ProfileSearchRequest req = objectMapper.readValue(json, ProfileSearchRequest.class);

        assertNotNull(req.getInclude());
        assertEquals(2, req.getInclude().size());
        assertTrue(req.getInclude().contains(ProfileIncludeEntity.PROFILE));
        assertTrue(req.getInclude().contains(ProfileIncludeEntity.ADDRESS));

        Set<ProfileIncludeEntity> resolved = profileService.resolveIncludes(req.getInclude());
        assertEquals(2, resolved.size());
        assertTrue(resolved.contains(ProfileIncludeEntity.PROFILE));
        assertTrue(resolved.contains(ProfileIncludeEntity.ADDRESS));
    }

    @Test
    @DisplayName("Should reject unknown include value in fromString with HTTP 400 CustomException")
    public void testFromStringWithUnknownValueThrowsCustomException() {
        CustomException exception = assertThrows(CustomException.class, () -> ProfileIncludeEntity.fromString("FOO"));
        assertEquals("INVALID_INCLUDE_ENTITY", exception.getCode());
        assertTrue(exception.getMessage().contains("FOO"));
    }

    @Test
    @DisplayName("Should reject invalid include entities during JSON deserialization")
    public void testDeserializationWithInvalidIncludeEntity_ThrowsException() {
        String json = "{\n" +
                "  \"tenantId\": \"pg.citya\",\n" +
                "  \"uuid\": [\"uuid-123\"],\n" +
                "  \"include\": [\"PROFILE\", \"INVALID_SECTION\"]\n" +
                "}";

        assertThrows(Exception.class, () -> objectMapper.readValue(json, ProfileSearchRequest.class));
    }

    @Test
    @DisplayName("Should reject citizen search for another citizen's UUID when fetching ADDRESS and DOCUMENT")
    public void testCitizenCannotFetchAddressOrDocumentForAnotherCitizen() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("another-citizen-uuid"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS, ProfileIncludeEntity.DOCUMENT))
                .requestInfo(RequestInfo.builder()
                        .userInfo(org.egov.common.contract.request.User.builder()
                                .uuid("logged-in-citizen-uuid")
                                .type("CITIZEN")
                                .roles(Collections.singletonList(org.egov.common.contract.request.Role.builder().code("CITIZEN").tenantId("pg").build()))
                                .build())
                        .build())
                .build();

        doThrow(new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH))
                .when(profileValidator).validateSearch(request);

        CustomException exception = assertThrows(CustomException.class, () -> profileService.searchProfile(request));
        assertEquals(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, exception.getCode());

        verify(addressService, never()).searchAddress(any(), any());
        verify(userDocumentService, never()).searchDocuments(any(), any());
    }

    @Test
    @DisplayName("Legacy constructor must not copy addresses or documents, avoiding PII exposure for existing search callers")
    public void testLegacyConstructor_DoesNotExposeAddressesOrDocuments() throws Exception {
        User user = createSampleUser("uuid-legacy");
        Address address = Address.builder().id(999L).city("Legacy City").build();
        user.setAddresses(Collections.singleton(address));
        UserDocument doc = UserDocument.builder().id("doc-1").documentType("IDENTITY_PROOF").build();
        user.setDocuments(Collections.singletonList(doc));

        // Legacy constructor used by /_search, /v1/_search, /users/v2/_search
        UserSearchResponseContent legacyContent = new UserSearchResponseContent(user);

        // Addresses and documents MUST remain null
        assertNull(legacyContent.getAddresses());
        assertNull(legacyContent.getDocuments());

        // Demographic and identity fields are mapped
        assertEquals("uuid-legacy", legacyContent.getUuid());
        assertEquals("Rohan Sharma", legacyContent.getName());
        assertEquals("9876543210", legacyContent.getUserName());

        // JSON serialization omits addresses and documents completely
        String json = objectMapper.writeValueAsString(legacyContent);
        assertFalse(json.contains("\"addresses\""));
        assertFalse(json.contains("\"documents\""));

        // Crucial contract compatibility check: null keys must NOT be removed by class-level NON_NULL
        // Existing clients (PT, TL, PGR, HRMS, UI) rely on null keys being present
        assertTrue(json.contains("\"emailId\":null"));
        assertTrue(json.contains("\"pan\":null"));
    }

    @Test
    @DisplayName("Should execute address and document enrichment for single user in searchProfile")
    public void testEnrichmentForSingleUser() {
        User u1 = createSampleUser("uuid-1").toBuilder().id(101L).build();

        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-1"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS, ProfileIncludeEntity.DOCUMENT))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(u1));

        Address addr1 = Address.builder().id(201L).city("City 1").userUuid("uuid-1").build();
        when(addressService.searchAddress(any(), any())).thenReturn(Collections.singletonList(addr1));

        UserDocument doc1 = UserDocument.builder().id("doc-1").documentType("IDENTITY_PROOF").userUuid("uuid-1").build();
        when(userDocumentService.searchDocuments(any(), any())).thenReturn(Collections.singletonList(doc1));

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        User res1 = results.get(0);

        verify(addressService, times(1)).searchAddress(any(AddressSearchCriteria.class), any());
        verify(userDocumentService, times(1)).searchDocuments(any(UserDocumentSearchCriteria.class), any());

        assertNotNull(res1.getAddresses());
        assertEquals(1, res1.getAddresses().size());
        assertNotNull(res1.getDocuments());
        assertEquals(1, res1.getDocuments().size());
    }

    @Test
    @DisplayName("Should return all matched users when multiple users match in database (e.g. mobile search)")
    public void testSearchProfileReturnsMultipleUsersWhenMatched() {
        User u1 = createSampleUser("uuid-1").toBuilder().id(101L).build();
        User u2 = createSampleUser("uuid-2").toBuilder().id(102L).name("User Two").build();

        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .mobileNumber("9876543210")
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Arrays.asList(u1, u2));

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("uuid-1", results.get(0).getUuid());
        assertEquals("uuid-2", results.get(1).getUuid());
    }

    @Test
    @DisplayName("Should throw CustomException with proper error message when address enrichment fails")
    public void testAddressEnrichmentFailureThrowsProperCustomException() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS))
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));

        when(addressService.searchAddress(any(), any())).thenThrow(new RuntimeException("Database connection timeout"));

        CustomException exception = assertThrows(CustomException.class, () -> profileService.searchProfile(request));
        assertEquals("ADDRESS_ENRICHMENT_FAILED", exception.getCode());
        assertTrue(exception.getMessage().contains("Failed to enrich address details"));
        assertTrue(exception.getMessage().contains("Database connection timeout"));
    }

    @Test
    @DisplayName("Should throw CustomException with proper error message when document enrichment fails")
    public void testDocumentEnrichmentFailureThrowsProperCustomException() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.DOCUMENT))
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));

        when(userDocumentService.searchDocuments(any(), any())).thenThrow(new RuntimeException("Filestore unavailable"));

        CustomException exception = assertThrows(CustomException.class, () -> profileService.searchProfile(request));
        assertEquals("DOCUMENT_ENRICHMENT_FAILED", exception.getCode());
        assertTrue(exception.getMessage().contains("Failed to enrich document proofs"));
        assertTrue(exception.getMessage().contains("Filestore unavailable"));
    }

    @Test
    @DisplayName("Should return partial response with demographics and error details when address enrichment fails")
    public void testPartialResponseWhenAddressEnrichmentFailsWithErrorsList() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS))
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));
        when(addressService.searchAddress(any(), any())).thenThrow(new RuntimeException("Database connection timeout"));

        List<org.egov.tracer.model.Error> errors = new ArrayList<>();
        Set<ProfileIncludeEntity> includes = new HashSet<>(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS));

        List<User> results = profileService.searchProfile(request, includes, errors);

        assertNotNull(results);
        assertEquals(1, results.size());
        User result = results.get(0);
        assertEquals("uuid-123", result.getUuid());
        assertNull(result.getAddresses());
        assertEquals(1, errors.size());
        assertEquals("ADDRESS_ENRICHMENT_FAILED", errors.get(0).getCode());
        assertTrue(errors.get(0).getMessage().contains("Database connection timeout"));
    }

    @Test
    @DisplayName("Should return partial response with demographics and error details when document enrichment fails")
    public void testPartialResponseWhenDocumentEnrichmentFailsWithErrorsList() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.DOCUMENT))
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));
        when(userDocumentService.searchDocuments(any(), any())).thenThrow(new RuntimeException("Filestore unavailable"));

        List<org.egov.tracer.model.Error> errors = new ArrayList<>();
        Set<ProfileIncludeEntity> includes = new HashSet<>(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.DOCUMENT));

        List<User> results = profileService.searchProfile(request, includes, errors);

        assertNotNull(results);
        assertEquals(1, results.size());
        User result = results.get(0);
        assertEquals("uuid-123", result.getUuid());
        assertNull(result.getDocuments());
        assertEquals(1, errors.size());
        assertEquals("DOCUMENT_ENRICHMENT_FAILED", errors.get(0).getCode());
        assertTrue(errors.get(0).getMessage().contains("Filestore unavailable"));
    }

    @Test
    @DisplayName("Should return documents and record address error when address fails but document succeeds")
    public void testAddressFailureDoesNotBlockDocumentEnrichment() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS, ProfileIncludeEntity.DOCUMENT))
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));
        when(addressService.searchAddress(any(), any())).thenThrow(new RuntimeException("DB down"));

        UserDocument doc = UserDocument.builder().id("10").userUuid("uuid-123").documentType("AADHAAR").build();
        when(userDocumentService.searchDocuments(any(), any())).thenReturn(Collections.singletonList(doc));

        List<org.egov.tracer.model.Error> errors = new ArrayList<>();
        Set<ProfileIncludeEntity> includes = new HashSet<>(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS, ProfileIncludeEntity.DOCUMENT));

        List<User> results = profileService.searchProfile(request, includes, errors);

        assertNotNull(results);
        assertEquals(1, results.size());
        User result = results.get(0);
        assertNull(result.getAddresses());
        assertNotNull(result.getDocuments());
        assertEquals(1, result.getDocuments().size());
        assertEquals("AADHAAR", result.getDocuments().get(0).getDocumentType());

        assertEquals(1, errors.size());
        assertEquals("ADDRESS_ENRICHMENT_FAILED", errors.get(0).getCode());
        assertTrue(errors.get(0).getMessage().contains("DB down"));
    }

    @Test
    @DisplayName("Should only return active documents when active is true in ProfileSearchRequest")
    public void testProfileSearch_WhenActiveIsTrue_EnrichesOnlyActiveDocuments() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.DOCUMENT))
                .active(true)
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));

        UserDocument docActive = UserDocument.builder().id("1").userUuid("uuid-123").documentType("DOB_PROOF").active(true).build();
        UserDocument docInactive = UserDocument.builder().id("2").userUuid("uuid-123").documentType("DOB_PROOF").active(false).build();
        when(userDocumentService.searchDocuments(any(), any())).thenReturn(Arrays.asList(docActive, docInactive));

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        User result = results.get(0);
        assertNotNull(result.getDocuments());
        assertEquals(1, result.getDocuments().size());
        assertEquals("1", result.getDocuments().get(0).getId());
        assertEquals(Boolean.TRUE, result.getDocuments().get(0).getActive());
    }

    @Test
    @DisplayName("Should only return active addresses when active is true in ProfileSearchRequest")
    public void testProfileSearch_WhenActiveIsTrue_EnrichesOnlyActiveAddresses() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS))
                .active(true)
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));

        Address addrActive = Address.builder().id(201L).userUuid("uuid-123").city("Active City").status("active").build();
        Address addrInactive = Address.builder().id(202L).userUuid("uuid-123").city("Inactive City").status("inactive").build();
        when(addressService.searchAddress(any(), any())).thenReturn(Arrays.asList(addrActive, addrInactive));

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        User result = results.get(0);
        assertNotNull(result.getAddresses());
        assertEquals(1, result.getAddresses().size());
        Address resultAddr = result.getAddresses().iterator().next();
        assertEquals(201L, resultAddr.getId());
        assertEquals("active", resultAddr.getStatus());
    }

    @Test
    @DisplayName("Should only return active addresses and active documents when active is true in ProfileSearchRequest")
    public void testProfileSearch_WhenActiveIsTrue_EnrichesOnlyActiveAddressesAndDocuments() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList(ProfileIncludeEntity.PROFILE, ProfileIncludeEntity.ADDRESS, ProfileIncludeEntity.DOCUMENT))
                .active(true)
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));

        Address addrActive = Address.builder().id(201L).userUuid("uuid-123").city("Active City").status("active").build();
        Address addrInactive = Address.builder().id(202L).userUuid("uuid-123").city("Inactive City").status("inactive").build();
        when(addressService.searchAddress(any(), any())).thenReturn(Arrays.asList(addrActive, addrInactive));

        UserDocument docActive = UserDocument.builder().id("1").userUuid("uuid-123").documentType("DOB_PROOF").active(true).build();
        UserDocument docInactive = UserDocument.builder().id("2").userUuid("uuid-123").documentType("DOB_PROOF").active(false).build();
        when(userDocumentService.searchDocuments(any(), any())).thenReturn(Arrays.asList(docActive, docInactive));

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        User result = results.get(0);

        // Verify active-only addresses
        assertNotNull(result.getAddresses());
        assertEquals(1, result.getAddresses().size());
        Address resultAddr = result.getAddresses().iterator().next();
        assertEquals(201L, resultAddr.getId());
        assertEquals("active", resultAddr.getStatus());

        // Verify active-only documents
        assertNotNull(result.getDocuments());
        assertEquals(1, result.getDocuments().size());
        UserDocument resultDoc = result.getDocuments().get(0);
        assertEquals("1", resultDoc.getId());
        assertEquals(Boolean.TRUE, resultDoc.getActive());
    }

    @Test
    @DisplayName("Should enrich familyId dynamically from FamilyRestRepository when available")
    public void testProfileSearch_EnrichesFamilyIdFromFamilyService() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Collections.singletonList(ProfileIncludeEntity.PROFILE))
                .build();

        User user = createSampleUser("uuid-123");
        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.singletonList(user));
        when(familyRestRepository.getActiveFamilyIdByUserUuid(eq("uuid-123"), anyString(), any()))
                .thenReturn("FAM-2024-00123");

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        User result = results.get(0);
        assertEquals("FAM-2024-00123", result.getFamilyId());

        // Verify mapping into UserSearchResponseContent
        UserSearchResponseContent content = new UserSearchResponseContent(result, Collections.singleton(ProfileIncludeEntity.PROFILE));
        assertEquals("FAM-2024-00123", content.getFamilyId());
    }

    @Test
    @DisplayName("Should return empty list when no user matches in database")
    public void testProfileSearch_WhenNoUserMatches_ReturnsEmptyList() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-nonexistent"))
                .build();

        when(userRepository.findAll(any(UserSearchCriteria.class))).thenReturn(Collections.emptyList());

        List<User> results = profileService.searchProfile(request);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
