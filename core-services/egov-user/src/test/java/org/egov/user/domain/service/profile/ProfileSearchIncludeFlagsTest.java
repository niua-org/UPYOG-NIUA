package org.egov.user.domain.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.service.address.AddressService;
import org.egov.user.domain.service.document.UserDocumentService;
import org.egov.user.domain.service.identity.IdentityVerificationService;
import org.egov.user.domain.service.profile.validator.ProfileValidator;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.UserSearchResponseContent;
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

        Set<String> includes = Collections.singleton("ADDRESS");
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

        // Verify JSON serialization omits null demographic fields
        String json = objectMapper.writeValueAsString(content);
        assertFalse(json.contains("\"name\""));
        assertFalse(json.contains("\"mobileNumber\""));
        assertFalse(json.contains("\"fatherName\""));
        assertFalse(json.contains("\"motherName\""));
        assertFalse(json.contains("\"documents\""));
        assertTrue(json.contains("\"addresses\""));
        assertTrue(json.contains("\"userName\":\"9876543210\""));
    }

    @Test
    @DisplayName("Should omit addresses and documents from JSON when null using Jackson serialization")
    public void testJacksonSerializationOmitsNullAddressesAndDocuments() throws Exception {
        User user = createSampleUser("uuid-123");
        UserSearchResponseContent content = new UserSearchResponseContent(user, Collections.singleton("PROFILE"));

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
    }
}
