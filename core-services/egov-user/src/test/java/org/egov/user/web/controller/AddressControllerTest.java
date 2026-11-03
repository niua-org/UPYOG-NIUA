package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.service.address.AddressMasterDataService;
import org.egov.user.domain.service.address.AddressService;
import org.egov.user.web.contract.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AddressControllerTest {

    private AddressService addressService;
    private AddressMasterDataService addressMasterDataService;
    private AddressController addressController;

    @BeforeEach
    public void setup() {
        addressService = mock(AddressService.class);
        addressMasterDataService = mock(AddressMasterDataService.class);
        addressController = new AddressController(addressService, addressMasterDataService);
    }

    @Test
    @DisplayName("Should refresh address type cache for specific tenant")
    public void testRefreshAddressTypeCache_WithTenant() {
        Set<String> expectedTypes = new HashSet<>(Arrays.asList("PERMANENT", "CORRESPONDENCE", "OTHER"));
        when(addressMasterDataService.refreshCache(any(RequestInfo.class), eq("pb.amritsar")))
                .thenReturn(expectedTypes);
        when(addressMasterDataService.getCachedAddressTypes())
                .thenReturn(Collections.singletonMap("pg", expectedTypes));

        AddressCacheRefreshRequest request = AddressCacheRefreshRequest.builder()
                .requestInfo(new RequestInfo())
                .tenantId("pb.amritsar")
                .build();

        ResponseEntity<AddressCacheRefreshResponse> response = addressController.refreshAddressTypeCache(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("pb.amritsar", response.getBody().getTenantId());
        assertEquals(expectedTypes, response.getBody().getAllowedAddressTypes());
        assertTrue(response.getBody().getMessage().contains("pb.amritsar"));
        verify(addressMasterDataService, times(1)).refreshCache(any(RequestInfo.class), eq("pb.amritsar"));
    }

    @Test
    @DisplayName("Should clear address type cache globally when tenantId is not supplied")
    public void testRefreshAddressTypeCache_GlobalClear() {
        AddressCacheRefreshRequest request = AddressCacheRefreshRequest.builder()
                .requestInfo(new RequestInfo())
                .build();

        ResponseEntity<AddressCacheRefreshResponse> response = addressController.refreshAddressTypeCache(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("globally"));
        verify(addressMasterDataService, times(1)).clearCache();
    }

    @Test
    @DisplayName("Should create address and return CREATED status")
    public void testCreateAddress() {
        Address created = Address.builder()
                .id(101L)
                .addressType("PERMANENT")
                .city("Amritsar")
                .build();

        when(addressService.createAddress(any(AddressRequest.class))).thenReturn(created);

        Address innerAddress = Address.builder()
                .addressType("PERMANENT")
                .city("Amritsar")
                .build();

        AddressRequest request = AddressRequest.builder()
                .uuid("usr-123")
                .address(innerAddress)
                .build();

        ResponseEntity<AddressResponse> response = addressController.createAddress(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, response.getBody().getAddress().size());
        assertEquals("Amritsar", response.getBody().getAddress().get(0).getCity());
    }

    @Test
    @DisplayName("Should search address and return OK status")
    public void testSearchAddress() {
        Address address = Address.builder()
                .id(101L)
                .city("Amritsar")
                .build();

        when(addressService.searchAddress(any(AddressSearchCriteria.class), any()))
                .thenReturn(Collections.singletonList(address));

        AddressSearchRequest request = AddressSearchRequest.builder()
                .uuid("usr-123")
                .tenantId("pb.amritsar")
                .build();

        ResponseEntity<AddressResponse> response = addressController.searchAddress(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getAddress().size());
    }

    @Test
    @DisplayName("Should delete address and return OK status")
    public void testDeleteAddress() {
        Address deleted = Address.builder()
                .id(101L)
                .status("INACTIVE")
                .build();

        when(addressService.deleteAddress(any(AddressRequest.class))).thenReturn(deleted);

        AddressRequest request = AddressRequest.builder()
                .addressId("101")
                .build();

        ResponseEntity<AddressResponse> response = addressController.deleteAddress(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getAddress().size());
        assertTrue("inactive".equalsIgnoreCase(response.getBody().getAddress().get(0).getStatus()));
    }
}
