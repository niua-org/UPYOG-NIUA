package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.dto.VendorTanVerificationRequest;
import org.egov.dx.adapter.vendor.tan.impl.ItdTanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.TanConstants;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItdTanGatewayAdapterTest {

    /**
     * Tests Constructor under the missing connect timeout_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing connect timeout_throws custom exception.
     */
    @Test
    void testConstructor_MissingConnectTimeout_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getTanVerificationConnectTimeout()).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> new ItdTanGatewayAdapter(config));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("tan.verification.connect.timeout is not configured"));
    }

    /**
     * Tests Constructor under the missing read timeout_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing read timeout_throws custom exception.
     */
    @Test
    void testConstructor_MissingReadTimeout_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getTanVerificationConnectTimeout()).thenReturn(5000);
        when(config.getTanVerificationReadTimeout()).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> new ItdTanGatewayAdapter(config));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("tan.verification.read.timeout is not configured"));
    }

    /**
     * Tests Constructor under the missing base url_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing base url_throws custom exception.
     */
    @Test
    void testConstructor_MissingBaseUrl_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getTanVerificationConnectTimeout()).thenReturn(5000);
        when(config.getTanVerificationReadTimeout()).thenReturn(10000);
        when(config.getTanVerificationBaseUrl()).thenReturn("");

        CustomException ce = assertThrows(CustomException.class, () -> new ItdTanGatewayAdapter(config));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("tan.verification.base.url is not configured"));
    }

    /**
     * Tests Verify Tan under the missing path_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing path_throws custom exception.
     */
    @Test
    void testVerifyTan_MissingPath_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getTanVerificationConnectTimeout()).thenReturn(5000);
        when(config.getTanVerificationReadTimeout()).thenReturn(10000);
        when(config.getTanVerificationBaseUrl()).thenReturn("https://uatservices.incometax.gov.in/services/auth/v0.1/");
        when(config.getTanVerificationPath()).thenReturn(null);

        ItdTanGatewayAdapter adapter = new ItdTanGatewayAdapter(config);
        VendorTanVerificationRequest request = VendorTanVerificationRequest.builder().build();

        CustomException ce = assertThrows(CustomException.class, () -> adapter.verifyTan(request));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("tan.verification.path is not configured"));
    }

    /**
     * Tests Verify Tan under the missing credentials_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing credentials_throws custom exception.
     */
    @Test
    void testVerifyTan_MissingCredentials_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getTanVerificationConnectTimeout()).thenReturn(5000);
        when(config.getTanVerificationReadTimeout()).thenReturn(10000);
        when(config.getTanVerificationBaseUrl()).thenReturn("https://uatservices.incometax.gov.in/services/auth/v0.1/");
        when(config.getTanVerificationPath()).thenReturn("tan/verify");
        when(config.getTanVerificationClientId()).thenReturn(null);

        ItdTanGatewayAdapter adapter = new ItdTanGatewayAdapter(config);
        VendorTanVerificationRequest request = VendorTanVerificationRequest.builder().build();

        CustomException ce = assertThrows(CustomException.class, () -> adapter.verifyTan(request));
        assertEquals(TanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("tan.verification.client.id is not configured"));
    }
}
