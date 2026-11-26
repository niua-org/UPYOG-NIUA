package org.egov.user.persistence.repository;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.config.UserConfiguration;
import org.egov.user.web.contract.property.UrbanPropertyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating property-services REST integration, response parsing, and owner mapping in {@link PropertyServiceClient}.
 */
public class PropertyServiceClientTest {

    private RestTemplate restTemplate;
    private UserConfiguration userConfiguration;
    private PropertyServiceClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        userConfiguration = mock(UserConfiguration.class);

        when(userConfiguration.getPropertyServiceHost()).thenReturn("http://localhost:8280/");
        when(userConfiguration.getPropertyServiceSearchEndpoint()).thenReturn("property-services/property/_search");

        client = new PropertyServiceClient(restTemplate, userConfiguration, null);
    }

    /**
     * Verifies that property tax JSON responses with assessment details and owner rosters are parsed correctly.
     */
    @Test
    @DisplayName("Should parse and map property response from property-services correctly")
    void testFetchPropertyById_Success() {
        String mockResponse = "{\n" +
                "  \"Properties\": [\n" +
                "    {\n" +
                "      \"propertyId\": \"PT-101\",\n" +
                "      \"tenantId\": \"pb.amritsar\",\n" +
                "      \"status\": \"ACTIVE\",\n" +
                "      \"ownershipCategory\": \"INDIVIDUAL.SINGLEOWNER\",\n" +
                "      \"address\": {\n" +
                "        \"doorNo\": \"123\",\n" +
                "        \"street\": \"Mall Road\",\n" +
                "        \"city\": \"Amritsar\"\n" +
                "      },\n" +
                "      \"owners\": [\n" +
                "        {\n" +
                "          \"uuid\": \"owner-1\",\n" +
                "          \"name\": \"Ramesh Kumar\",\n" +
                "          \"mobileNumber\": \"9876543210\",\n" +
                "          \"status\": \"ACTIVE\",\n" +
                "          \"isPrimaryOwner\": true\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(mockResponse);

        UrbanPropertyInfo info = client.fetchPropertyById("pb.amritsar", "PT-101", new RequestInfo());

        assertNotNull(info);
        assertEquals("PT-101", info.getPropertyId());
        assertEquals("INDIVIDUAL.SINGLEOWNER", info.getOwnershipCategory());
        assertEquals(1, info.getOwners().size());
        assertEquals("Ramesh Kumar", info.getOwners().get(0).getName());
        assertEquals("9876543210", info.getOwners().get(0).getRawMobileNumber());
        assertEquals("98******10", info.getOwners().get(0).getMaskedMobileNumber());
    }

    /**
     * Verifies that when property-services returns an empty Properties array, null is returned cleanly.
     */
    @Test
    @DisplayName("Should return null if empty Properties array is returned")
    void testFetchPropertyById_EmptyList_ReturnsNull() {
        String mockResponse = "{\"Properties\": []}";
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenReturn(mockResponse);

        UrbanPropertyInfo info = client.fetchPropertyById("pb.amritsar", "PT-101", new RequestInfo());

        assertNull(info);
    }
}
