package org.egov.dx.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.Configurations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UserRegistryServiceImplTest {

    private RestTemplate restTemplate;
    private Configurations configurations;
    private ObjectMapper objectMapper;
    private UserRegistryServiceImpl userRegistryService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        configurations = mock(Configurations.class);
        objectMapper = new ObjectMapper();

        when(configurations.getUserHost()).thenReturn("http://localhost:8085/");

        userRegistryService = new UserRegistryServiceImpl(restTemplate, configurations, objectMapper);
        ReflectionTestUtils.setField(userRegistryService, "profileSearchPath", "/user/profile/_search");
        ReflectionTestUtils.setField(userRegistryService, "userSearchPath", "/user/_search");
        ReflectionTestUtils.setField(userRegistryService, "userCreatePath", "/user/users/_createnovalidate");
    }

    @Test
    void testSearchUserUuidByAadhaar_BlankAadhaar_ReturnsNull() {
        String uuid = userRegistryService.searchUserUuidByAadhaar("", "pg.citya", new RequestInfo());
        assertNull(uuid);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testSearchUserUuidByAadhaar_MatchesVerifiedCitizen_ReturnsUuid() {
        String mockResponse = "{\n" +
                "  \"responseInfo\": {\"status\": \"SUCCESS\"},\n" +
                "  \"user\": [\n" +
                "    {\n" +
                "      \"id\": 9154,\n" +
                "      \"uuid\": \"0b90ae1c-7506-4114-b409-d48093e5db45\",\n" +
                "      \"active\": true,\n" +
                "      \"isIdentityVerified\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        String uuid = userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertEquals("0b90ae1c-7506-4114-b409-d48093e5db45", uuid);
    }

    @Test
    void testSearchUserUuidByAadhaar_EmptyUserList_RetriesMasked_ReturnsNullIfNotFound() {
        String emptyResponse = "{\"responseInfo\":{\"status\":\"SUCCESS\"},\"user\":[]}";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(emptyResponse);

        String uuid = userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertNull(uuid);
        // Verified it tried both raw and masked
        verify(restTemplate, atLeast(2)).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testSearchUserUuidByAadhaar_HttpException_ReturnsNullSafely() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String uuid = userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertNull(uuid);
    }

    @Test
    void testResolveOrCreateUserUuid_CitizenExists_ReturnsExistingUuid() {
        String mockSearchResponse = "{\n" +
                "  \"responseInfo\": {\"status\": \"SUCCESS\"},\n" +
                "  \"user\": [\n" +
                "    {\n" +
                "      \"id\": 9154,\n" +
                "      \"uuid\": \"existing-citizen-uuid-123\",\n" +
                "      \"active\": true,\n" +
                "      \"isIdentityVerified\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockSearchResponse);

        org.egov.dx.web.models.UserDemographics demographics = org.egov.dx.web.models.UserDemographics.builder()
                .name("Existing Citizen")
                .dob("1990-01-01")
                .gender("M")
                .build();

        String uuid = userRegistryService.resolveOrCreateUserUuid("548796521458", demographics, "pg.citya", new RequestInfo());
        assertEquals("existing-citizen-uuid-123", uuid);
    }

    @Test
    void testResolveOrCreateUserUuid_CitizenDoesNotExist_AutoProvisionsAndReturnsNewUuid() {
        String emptySearchResponse = "{\"responseInfo\":{\"status\":\"SUCCESS\"},\"user\":[]}";
        String createResponse = "{\n" +
                "  \"responseInfo\": {\"status\": \"200\"},\n" +
                "  \"user\": [\n" +
                "    {\n" +
                "      \"id\": 9219,\n" +
                "      \"uuid\": \"newly-created-citizen-uuid-456\",\n" +
                "      \"active\": true,\n" +
                "      \"isIdentityVerified\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(contains("_search"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(emptySearchResponse);
        when(restTemplate.postForObject(contains("_createnovalidate"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponse);

        org.egov.dx.web.models.UserDemographics demographics = org.egov.dx.web.models.UserDemographics.builder()
                .name("New Citizen")
                .dob("1995-05-15")
                .gender("M")
                .fullAddress("123, Model Town, City, State, 143001")
                .city("Amritsar")
                .pincode("143001")
                .build();

        String uuid = userRegistryService.resolveOrCreateUserUuid("548796521458", demographics, "pg.citya", new RequestInfo());
        assertEquals("newly-created-citizen-uuid-456", uuid);
        verify(restTemplate, times(1)).postForObject(contains("_createnovalidate"), any(HttpEntity.class), eq(String.class));
    }
}
