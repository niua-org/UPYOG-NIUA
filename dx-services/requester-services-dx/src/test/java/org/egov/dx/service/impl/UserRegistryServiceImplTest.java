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

class UserRegistryServiceImplTest {

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
        String mockResponse = """
                {
                  "responseInfo": {"status": "SUCCESS"},
                  "user": [
                    {
                      "id": 9154,
                      "uuid": "0b90ae1c-7506-4114-b409-d48093e5db45",
                      "active": true,
                      "aadhaarNumber": "548796521458",
                      "isIdentityVerified": true
                    }
                  ]
                }""";

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
        String mockResponse = """
                {
                  "responseInfo": {"status": "SUCCESS"},
                  "user": [
                    {
                      "uuid": "legacy-user",
                      "active": true,
                      "aadhaarNumber": "XXXXXXXX1458"
                    }
                  ]
                }""";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        String uuid = userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertNull(uuid);
    }

    @Test
    void testResolveOrCreateUserUuid_CitizenDoesNotExist_AutoProvisionsWithoutIdentityVerifiedFlag() {
        String emptySearchResponse = "{\"responseInfo\":{\"status\":\"SUCCESS\"},\"user\":[]}";
        String createResponse = """
                {
                  "responseInfo": {"status": "200"},
                  "user": [
                    {
                      "uuid": "newly-created-citizen-uuid-456",
                      "active": true
                    }
                  ]
                }""";

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

        RequestInfo requestInfo = new RequestInfo();
        assertThrows(CustomException.class, () ->
                userRegistryService.resolveOrCreateUserUuid("invalid", demographics, "pg.citya", requestInfo));
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
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(CustomException.class, () ->
                userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", requestInfo));
    }

    @Test
    void testSearchUserUuidByAadhaar_MissingSearchPath_ThrowsCustomException() {
        ReflectionTestUtils.setField(userRegistryService, "aadhaarSearchPath", "");
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(CustomException.class, () ->
                userRegistryService.searchUserUuidByAadhaar("548796521458", "pg.citya", requestInfo));
    }

    @Test
    void testResolveOrCreateUserUuid_MissingCreatePath_ThrowsCustomException() {
        ReflectionTestUtils.setField(userRegistryService, "userCreatePath", " ");
        org.egov.dx.web.models.UserDemographics demographics = org.egov.dx.web.models.UserDemographics.builder()
                .name("Citizen")
                .build();
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(CustomException.class, () ->
                userRegistryService.resolveOrCreateUserUuid("548796521458", demographics, "pg.citya", requestInfo));
    }

    @Test
    void testMarkCitizenAadhaarVerifiedAfterEkyc_MissingMarkVerifiedPath_ThrowsCustomException() {
        ReflectionTestUtils.setField(userRegistryService, "markVerifiedPath", null);
        RequestInfo requestInfo = new RequestInfo();
        assertThrows(CustomException.class, () ->
                userRegistryService.markCitizenAadhaarVerifiedAfterEkyc("uuid-1", "548796521458", "pg.citya", requestInfo));
    }

    @Test
    void testSearchCitizenByAadhaar_MatchesFullAadhaar_ReturnsRecordWithUuidAndFamilyId() {
        String mockResponse = """
                {
                  "responseInfo": {"status": "SUCCESS"},
                  "user": [
                    {
                      "id": 9154,
                      "uuid": "0b90ae1c-7506-4114-b409-d48093e5db45",
                      "active": true,
                      "aadhaarNumber": "548796521458",
                      "familyId": "FAM-2026-F638A3E1",
                      "isIdentityVerified": true
                    }
                  ]
                }""";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        UserRegistryRecord userRecord = userRegistryService.searchCitizenByAadhaar("548796521458", "pg.citya", new RequestInfo());
        assertEquals("0b90ae1c-7506-4114-b409-d48093e5db45", userRecord.getUserUuid());
        assertEquals("FAM-2026-F638A3E1", userRecord.getFamilyId());
        assertEquals("FAM-2026-F638A3E1", userRegistryService.searchFamilyIdByAadhaar("548796521458", "pg.citya", new RequestInfo()));
    }

    @Test
    void testSearchFamilyIdByUserUuid_WithFamilyServiceHost_QueriesFamilyServiceDirectly() {
        when(configurations.getFamilyServiceHost()).thenReturn("http://localhost:8092/");
        when(configurations.getFamilyServiceSearchEndpoint()).thenReturn("family/v1/family/_search");

        String mockFamilyResponse = """
                {
                  "families": [
                    {
                      "familyId": "FAM-DIRECT-123",
                      "status": "ACTIVE"
                    }
                  ]
                }""";

        when(restTemplate.postForObject(contains("family/v1/family/_search"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockFamilyResponse);

        String familyId = userRegistryService.searchFamilyIdByUserUuid("usr-123", "pg.citya", new RequestInfo());
        assertEquals("FAM-DIRECT-123", familyId);
    }

    @Test
    void testSearchFamilyIdByUserUuid_WithoutFamilyServiceHost_QueriesProfileSearch() {
        when(configurations.getFamilyServiceHost()).thenReturn(null);

        String mockProfileResponse = """
                {
                  "user": [
                    {
                      "uuid": "usr-123",
                      "familyId": "FAM-FROM-PROFILE-456"
                    }
                  ]
                }""";

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
        org.junit.jupiter.api.Assertions.assertTrue(assignedMobile.matches("^[6-9]\\d{9}$"),
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
