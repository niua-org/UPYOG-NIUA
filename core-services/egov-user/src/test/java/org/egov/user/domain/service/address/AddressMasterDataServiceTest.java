package org.egov.user.domain.service.address;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AddressMasterDataServiceTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private UserConfiguration userConfiguration;
    private AddressMasterDataServiceImpl addressMasterDataService;

    @BeforeEach
    public void setup() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        userConfiguration = UserConfiguration.builder()
                .mdmsAddressTypeEnabled(false)
                .allowedAddressTypes("PERMANENT,CORRESPONDENCE,OTHER,OFFICE")
                .addressTypeCacheTtlMillis(86400000L) // 24 hours
                .addressTypeCacheCron("0 0 0 * * *")
                .stateLevelTenantId("pg")
                .build();

        addressMasterDataService = new AddressMasterDataServiceImpl(restTemplate, objectMapper, userConfiguration);
    }

    @Test
    @DisplayName("Should load allowed address types from UserConfiguration when MDMS is disabled")
    public void testGetAllowedAddressTypes_Fallback() {
        Set<String> types = addressMasterDataService.getAllowedAddressTypes(new RequestInfo(), "pb.amritsar");
        assertNotNull(types);
        assertTrue(types.contains("PERMANENT"));
        assertTrue(types.contains("CORRESPONDENCE"));
        assertTrue(types.contains("OTHER"));
        assertTrue(types.contains("OFFICE"));
    }

    @Test
    @DisplayName("Should cache allowed address types and return cached instance on subsequent calls")
    public void testAddressTypeCaching() {
        // Initial fetch populates cache
        Set<String> initialTypes = addressMasterDataService.getAllowedAddressTypes(new RequestInfo(), "pb.amritsar");
        assertNotNull(initialTypes);

        // Verify cache snapshot contains tenant 'pb'
        Map<String, Set<String>> cached = addressMasterDataService.getCachedAddressTypes();
        assertTrue(cached.containsKey("pb"));
        assertEquals(initialTypes, cached.get("pb"));

        // Subsequent call returns cached types
        Set<String> cachedTypes = addressMasterDataService.getAllowedAddressTypes(new RequestInfo(), "pb.amritsar");
        assertSame(initialTypes, cachedTypes);
    }

    @Test
    @DisplayName("Should manually clear and refresh cache on demand")
    public void testClearAndRefreshCache() {
        // Populate cache
        addressMasterDataService.getAllowedAddressTypes(new RequestInfo(), "pb.amritsar");
        assertFalse(addressMasterDataService.getCachedAddressTypes().isEmpty());

        // Clear cache
        addressMasterDataService.clearCache();
        assertTrue(addressMasterDataService.getCachedAddressTypes().isEmpty());

        // Manually refresh cache for tenant
        Set<String> refreshed = addressMasterDataService.refreshCache(new RequestInfo(), "pb.amritsar");
        assertNotNull(refreshed);
        assertTrue(addressMasterDataService.getCachedAddressTypes().containsKey("pb"));

        // Scheduled eviction clears entries
        addressMasterDataService.scheduledDailyCacheEviction();
        assertTrue(addressMasterDataService.getCachedAddressTypes().isEmpty());
    }

    @Test
    @DisplayName("Should successfully validate valid configured address types")
    public void testValidateAddressType_Valid() {
        assertDoesNotThrow(() ->
                addressMasterDataService.validateAddressType(new RequestInfo(), "pb.amritsar", "PERMANENT")
        );
        assertDoesNotThrow(() ->
                addressMasterDataService.validateAddressType(new RequestInfo(), "pb.amritsar", "correspondence")
        );
        assertDoesNotThrow(() ->
                addressMasterDataService.validateAddressType(new RequestInfo(), "pb.amritsar", "CORRESPONDANCE") // Alias
        );
        assertDoesNotThrow(() ->
                addressMasterDataService.validateAddressType(new RequestInfo(), "pb.amritsar", "OFFICE")
        );
    }

    @Test
    @DisplayName("Should throw CustomException when address type is invalid")
    public void testValidateAddressType_Invalid() {
        CustomException ex = assertThrows(CustomException.class, () ->
                addressMasterDataService.validateAddressType(new RequestInfo(), "pb.amritsar", "INVALID_TYPE")
        );
        assertEquals("INVALID_ADDRESS_TYPE", ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when address type is null or blank")
    public void testValidateAddressType_NullOrBlank() {
        CustomException ex = assertThrows(CustomException.class, () ->
                addressMasterDataService.validateAddressType(new RequestInfo(), "pb.amritsar", "")
        );
        assertEquals("MANDATORY_FIELD_MISSING", ex.getCode());
    }

    @Test
    @DisplayName("Should throw CustomException when tenantId is blank and stateLevelTenantId is null")
    public void testGetAllowedAddressTypes_NullTenantAndNullConfig_ThrowsException() {
        UserConfiguration emptyConfig = UserConfiguration.builder()
                .mdmsAddressTypeEnabled(false)
                .allowedAddressTypes("PERMANENT,CORRESPONDENCE")
                .stateLevelTenantId(null)
                .build();
        AddressMasterDataServiceImpl serviceWithNullConfig = new AddressMasterDataServiceImpl(restTemplate, objectMapper, emptyConfig);

        CustomException ex = assertThrows(CustomException.class, () ->
                serviceWithNullConfig.getAllowedAddressTypes(new RequestInfo(), null)
        );
        assertEquals("TENANT_ID_REQUIRED", ex.getCode());
        assertTrue(ex.getMessage().contains("state-level tenant ID is not configured"));
    }
}
