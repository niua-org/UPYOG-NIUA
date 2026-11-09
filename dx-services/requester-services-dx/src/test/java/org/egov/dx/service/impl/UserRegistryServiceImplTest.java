package org.egov.dx.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.util.Configurations;
import org.egov.dx.web.models.UserRegistryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import org.egov.tracer.model.CustomException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        ReflectionTestUtils.setField(userRegistryService, "aadhaarSearchPath", "/user/profile/aadhaar/_search");
        ReflectionTestUtils.setField(userRegistryService, "profileSearchPath", "/user/profile/_search");
        ReflectionTestUtils.setField(userRegistryService, "userSearchPath", "/user/_search");
        ReflectionTestUtils.setField(userRegistryService, "userCreatePath", "/user/users/_createnovalidate");
        ReflectionTestUtils.setField(userRegistryService, "markVerifiedPath", "/user/profile/aadhaar/_markVerifiedAfterEkyc");
    }

    @Test
    void testSearchUserUuidByAadhaar_BlankAadhaar_ReturnsNull() {
        String uuid = userRegistryService.searchUserUuidByAadhaar("", "pg.citya", new RequestInfo());
        assertNull(uuid);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testSearchUserUuidByAadhaar_MatchesFullAadhaar_ReturnsUuid() {
        String mockResponse = "{\n" +
                "  \"responseInfo\": {\"status\": \"SUCCESS\"},\n" +
                "  \"user\": [\n" +
                "    {\n" +
                "      \"id\": 9154,\n" +
                "      \"uuid\": \"0b90ae1c-7506-4114-b409-d48093e5db45\",\n" +
                "      \"active\": true,\n" +
                "      \"aadhaarNumber\": \"548796521458\",\n" +
                "      \"isIdentityVerified\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        String uuid = userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertEquals("0b90ae1c-7506-4114-b409-d48093e5db45", uuid);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(String.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("548796521458", body.get("aadhaarNumber"));
    }

    @Test
    void testSearchUserUuidByAadhaar_LastFourOnlyMaskedRecord_DoesNotMatch() {
        String mockResponse = "{\n" +
                "  \"responseInfo\": {\"status\": \"SUCCESS\"},\n" +
                "  \"user\": [\n" +
                "    {\n" +
                "      \"uuid\": \"legacy-user\",\n" +
                "      \"active\": true,\n" +
                "      \"aadhaarNumber\": \"XXXXXXXX1458\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        String uuid = userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertNull(uuid);
    }

    @Test
    void testResolveOrCreateUserUuid_CitizenDoesNotExist_AutoProvisionsWithoutIdentityVerifiedFlag() {
        String emptySearchResponse = "{\"responseInfo\":{\"status\":\"SUCCESS\"},\"user\":[]}";
        String createResponse = "{\n" +
                "  \"responseInfo\": {\"status\": \"200\"},\n" +
                "  \"user\": [\n" +
                "    {\n" +
                "      \"uuid\": \"newly-created-citizen-uuid-456\",\n" +
                "      \"active\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(contains("_search"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(emptySearchResponse);
        when(restTemplate.postForObject(contains("_createnovalidate"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponse);

        org.egov.dx.web.models.UserDemographics demographics = org.egov.dx.web.models.UserDemographics.builder()
                .name("New Citizen")
                .build();

        String uuid = userRegistryService.resolveOrCreateUserUuid("548796521458", demographics, "pg.citya", new RequestInfo());
        assertEquals("newly-created-citizen-uuid-456", uuid);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(contains("_createnovalidate"), captor.capture(), eq(String.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertEquals("548796521458", user.get("aadhaarNumber"));
        assertNull(user.get("isIdentityVerified"));
    }

    @Test
    void testResolveOrCreateUserUuid_InvalidAadhaar_ThrowsCustomException() {
        org.egov.dx.web.models.UserDemographics demographics = org.egov.dx.web.models.UserDemographics.builder()
                .name("Citizen")
                .build();

        assertThrows(CustomException.class, () ->
                userRegistryService.resolveOrCreateUserUuid("invalid", demographics, "pg.citya", new RequestInfo()));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testSearchUserUuidByAadhaar_HttpException_ReturnsNullSafely() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String uuid = userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertNull(uuid);
    }

    @Test
    void testSearchUserUuidByAadhaar_MissingUserHost_ThrowsCustomException() {
        when(configurations.getUserHost()).thenReturn(null);
        assertThrows(CustomException.class, () ->
                userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo()));
    }

    @Test
    void testSearchUserUuidByAadhaar_MissingSearchPath_ThrowsCustomException() {
        ReflectionTestUtils.setField(userRegistryService, "aadhaarSearchPath", "");
        assertThrows(CustomException.class, () ->
                userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo()));
    }

    @Test
    void testResolveOrCreateUserUuid_MissingCreatePath_ThrowsCustomException() {
        ReflectionTestUtils.setField(userRegistryService, "userCreatePath", " ");
        org.egov.dx.web.models.UserDemographics demographics = org.egov.dx.web.models.UserDemographics.builder()
                .name("Citizen")
                .build();
        assertThrows(CustomException.class, () ->
                userRegistryService.resolveOrCreateUserUuid("548796521458", demographics, "pg.citya", new RequestInfo()));
    }

    @Test
    void testMarkCitizenAadhaarVerifiedAfterEkyc_MissingMarkVerifiedPath_ThrowsCustomException() {
        ReflectionTestUtils.setField(userRegistryService, "markVerifiedPath", null);
        assertThrows(CustomException.class, () ->
                userRegistryService.markCitizenAadhaarVerifiedAfterEkyc("uuid-1", "548796521458", "pg.citya", new RequestInfo()));
    }

    @Test
    void testSearchCitizenByAadhaar_MatchesFullAadhaar_ReturnsRecordWithUuidAndFamilyId() {
        String mockResponse = "{\n" +
                "  \"responseInfo\": {\"status\": \"SUCCESS\"},\n" +
                "  \"user\": [\n" +
                "    {\n" +
                "      \"id\": 9154,\n" +
                "      \"uuid\": \"0b90ae1c-7506-4114-b409-d48093e5db45\",\n" +
                "      \"active\": true,\n" +
                "      \"aadhaarNumber\": \"548796521458\",\n" +
                "      \"familyId\": \"FAM-2026-F638A3E1\",\n" +
                "      \"isIdentityVerified\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        UserRegistryRecord record = userRegistryService.searchCitizenByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertEquals("0b90ae1c-7506-4114-b409-d48093e5db45", record.getUserUuid());
        assertEquals("FAM-2026-F638A3E1", record.getFamilyId());
        assertEquals("FAM-2026-F638A3E1", userRegistryService.searchFamilyIdByAadhaar("548796521458", "pg.citya", new RequestInfo()));
    }

    @Test
    void testSearchFamilyIdByUserUuid_WithFamilyServiceHost_QueriesFamilyServiceDirectly() {
        when(configurations.getFamilyServiceHost()).thenReturn("http://localhost:8092/");
        when(configurations.getFamilyServiceSearchEndpoint()).thenReturn("family/v1/family/_search");

        String mockFamilyResponse = "{\n" +
                "  \"families\": [\n" +
                "    {\n" +
                "      \"familyId\": \"FAM-DIRECT-123\",\n" +
                "      \"status\": \"ACTIVE\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(contains("family/v1/family/_search"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockFamilyResponse);

        String familyId = userRegistryService.searchFamilyIdByUserUuid("usr-123", "pg.citya", new RequestInfo());
        assertEquals("FAM-DIRECT-123", familyId);
    }

    @Test
    void testSearchFamilyIdByUserUuid_WithoutFamilyServiceHost_QueriesProfileSearch() {
        when(configurations.getFamilyServiceHost()).thenReturn(null);

        String mockProfileResponse = "{\n" +
                "  \"user\": [\n" +
                "    {\n" +
                "      \"uuid\": \"usr-123\",\n" +
                "      \"familyId\": \"FAM-FROM-PROFILE-456\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(contains("/user/profile/_search"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockProfileResponse);

        String familyId = userRegistryService.searchFamilyIdByUserUuid("usr-123", "pg.citya", new RequestInfo());
        assertEquals("FAM-FROM-PROFILE-456", familyId);
    }

    @Test
    void testResolveOrCreateUserUuid_CitizenCreatedWithoutMobile_AssignsRandomMobile() {
        // Aadhaar search returns no existing user
        when(restTemplate.postForObject(contains("/user/profile/aadhaar/_search"), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"user\": []}");

        // /user/_search check returns no user (candidate mobile is unique)
        when(restTemplate.postForObject(contains("/user/_search"), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"user\": []}");

        // /user/users/_createnovalidate returns newly provisioned user
        String createResponse = "{\"user\": [{\"uuid\": \"new-uuid-random-mobile\", \"active\": true}]}";
        when(restTemplate.postForObject(contains("/user/users/_createnovalidate"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponse);

        org.egov.dx.web.models.UserDemographics demographics = org.egov.dx.web.models.UserDemographics.builder()
                .name("Test Citizen")
                .build();

        String uuid = userRegistryService.resolveOrCreateUserUuid("548796521458", demographics, "pg.citya", new RequestInfo());
        assertEquals("new-uuid-random-mobile", uuid);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(contains("/user/users/_createnovalidate"), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> reqBody = (Map<String, Object>) captor.getValue().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) reqBody.get("user");

        String assignedMobile = (String) userMap.get("mobileNumber");
        org.junit.jupiter.api.Assertions.assertNotNull(assignedMobile, "Mobile number must not be null");
        org.junit.jupiter.api.Assertions.assertTrue(assignedMobile.matches("^[6-9][0-9]{9}$"),
                "Generated mobile must be a valid 10-digit Indian mobile number");
        assertEquals(assignedMobile, userMap.get("userName"));
        assertEquals(assignedMobile, demographics.getMobileNumber());
    }

    @Test
    void testResolveOrCreateUserUuid_CitizenCreatedWithMobile_AssignsProvidedMobile() {
        when(restTemplate.postForObject(contains("/user/profile/aadhaar/_search"), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{\"user\": []}");

        String createResponse = "{\"user\": [{\"uuid\": \"new-uuid-provided-mobile\", \"active\": true}]}";
        when(restTemplate.postForObject(contains("/user/users/_createnovalidate"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(createResponse);

        org.egov.dx.web.models.UserDemographics demographics = org.egov.dx.web.models.UserDemographics.builder()
                .name("Test Citizen")
                .mobileNumber("9876543210")
                .build();

        String uuid = userRegistryService.resolveOrCreateUserUuid("548796521458", demographics, "pg.citya", new RequestInfo());
        assertEquals("new-uuid-provided-mobile", uuid);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(contains("/user/users/_createnovalidate"), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> reqBody = (Map<String, Object>) captor.getValue().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) reqBody.get("user");

        assertEquals("9876543210", userMap.get("mobileNumber"));
        assertEquals("9876543210", userMap.get("userName"));
    }
}
