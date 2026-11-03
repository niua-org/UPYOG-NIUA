package org.egov.user.domain.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.service.address.AddressService;
import org.egov.user.domain.service.document.UserDocumentService;
import org.egov.user.domain.service.identity.IdentityVerificationService;
import org.egov.user.domain.service.profile.validator.ProfileValidator;
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
 *   <li>Backwards-compatible alias deserialization (e.g. {@code flags})</li>
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
                userDocumentService
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
        assertEquals("Rohan Sharma", results.get(0).getName());
        assertNull(results.get(0).getAddresses());
        assertNull(results.get(0).getDocuments());

        verify(addressService, never()).searchAddress(any(), any());
        verify(userDocumentService, never()).searchDocuments(any(), any());
    }

    @Test
    @DisplayName("Should enrich ADDRESS and DOCUMENT when both flags are specified")
    public void testSearchProfileWithAddressAndDocument() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("uuid-123"))
                .include(Arrays.asList("PROFILE", "ADDRESS", "DOCUMENT"))
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
    @DisplayName("Should support alias 'flags' when deserializing ProfileSearchRequest")
    public void testDeserializationWithAliasFlags() throws Exception {
        String json = "{\n" +
                "  \"tenantId\": \"pg.citya\",\n" +
                "  \"uuid\": [\"uuid-123\"],\n" +
                "  \"flags\": [\"PROFILE\", \"ADDRESS\"]\n" +
                "}";

        ProfileSearchRequest req = objectMapper.readValue(json, ProfileSearchRequest.class);

        assertNotNull(req.getInclude());
        assertEquals(2, req.getInclude().size());
        assertTrue(req.getInclude().contains("PROFILE"));
        assertTrue(req.getInclude().contains("ADDRESS"));

        Set<ProfileIncludeEntity> resolved = profileService.resolveIncludes(req.getInclude());
        assertEquals(2, resolved.size());
        assertTrue(resolved.contains(ProfileIncludeEntity.PROFILE));
        assertTrue(resolved.contains(ProfileIncludeEntity.ADDRESS));
    }

    @Test
    @DisplayName("Should reject unknown include value with HTTP 400 CustomException")
    public void testResolveIncludesWithUnknownValueThrowsCustomException() {
        List<String> rawIncludes = Arrays.asList("PROFILE", "FOO");

        CustomException exception = assertThrows(CustomException.class, () -> profileService.resolveIncludes(rawIncludes));
        assertEquals("INVALID_INCLUDE_ENTITY", exception.getCode());
        assertTrue(exception.getMessage().contains("FOO"));
    }

    @Test
    @DisplayName("Should reject invalid include entities when resolving ProfileSearchRequest include")
    public void testSearchWithInvalidIncludeEntity_ThrowsCustomException() throws Exception {
        String json = "{\n" +
                "  \"tenantId\": \"pg.citya\",\n" +
                "  \"uuid\": [\"uuid-123\"],\n" +
                "  \"include\": [\"PROFILE\", \"INVALID_SECTION\"]\n" +
                "}";

        ProfileSearchRequest req = objectMapper.readValue(json, ProfileSearchRequest.class);
        assertNotNull(req.getInclude());

        CustomException ex = assertThrows(CustomException.class, () -> profileService.resolveIncludes(req.getInclude()));
        assertEquals("INVALID_INCLUDE_ENTITY", ex.getCode());
        assertTrue(ex.getMessage().contains("INVALID_SECTION"));
    }

    @Test
    @DisplayName("Should reject citizen search for another citizen's UUID when fetching ADDRESS and DOCUMENT")
    public void testCitizenCannotFetchAddressOrDocumentForAnotherCitizen() {
        ProfileSearchRequest request = ProfileSearchRequest.builder()
                .tenantId("pg.citya")
                .uuid(Collections.singletonList("another-citizen-uuid"))
                .include(Arrays.asList("PROFILE", "ADDRESS", "DOCUMENT"))
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
}
