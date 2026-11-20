package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.gst.impl.IrisGstGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.GstConstants;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IrisGstGatewayAdapterTest {

    /**
     * Tests Constructor under the missing connect timeout_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing connect timeout_throws custom exception.
     */
    @Test
    void testConstructor_MissingConnectTimeout_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getGstVerificationConnectTimeout()).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> new IrisGstGatewayAdapter(config));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("gst.verification.connect.timeout is not configured"));
    }

    /**
     * Tests Constructor under the missing read timeout_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing read timeout_throws custom exception.
     */
    @Test
    void testConstructor_MissingReadTimeout_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getGstVerificationConnectTimeout()).thenReturn(5000);
        when(config.getGstVerificationReadTimeout()).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> new IrisGstGatewayAdapter(config));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("gst.verification.read.timeout is not configured"));
    }

    /**
     * Tests Constructor under the missing base url_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing base url_throws custom exception.
     */
    @Test
    void testConstructor_MissingBaseUrl_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getGstVerificationConnectTimeout()).thenReturn(5000);
        when(config.getGstVerificationReadTimeout()).thenReturn(10000);
        when(config.getGstVerificationBaseUrl()).thenReturn("");

        CustomException ce = assertThrows(CustomException.class, () -> new IrisGstGatewayAdapter(config));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("gst.verification.base.url is not configured"));
    }

    /**
     * Tests Search Gstin under the missing path_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing path_throws custom exception.
     */
    @Test
    void testSearchGstin_MissingPath_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getGstVerificationConnectTimeout()).thenReturn(5000);
        when(config.getGstVerificationReadTimeout()).thenReturn(10000);
        when(config.getGstVerificationBaseUrl()).thenReturn("https://taxpayer.irisgst.com/");
        when(config.getGstVerificationPath()).thenReturn(null);

        IrisGstGatewayAdapter adapter = new IrisGstGatewayAdapter(config);

        CustomException ce = assertThrows(CustomException.class, () -> adapter.searchGstin("03AAAPS0001B1ZW"));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("gst.verification.path is not configured"));
    }

    /**
     * Tests Search Gstin under the missing api key_throws custom exception scenario.
     *
     * Verifies that {@code CustomException} is thrown when missing api key_throws custom exception.
     */
    @Test
    void testSearchGstin_MissingApiKey_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getGstVerificationConnectTimeout()).thenReturn(5000);
        when(config.getGstVerificationReadTimeout()).thenReturn(10000);
        when(config.getGstVerificationBaseUrl()).thenReturn("https://taxpayer.irisgst.com/");
        when(config.getGstVerificationPath()).thenReturn("api/search");
        when(config.getGstVerificationApiKey()).thenReturn(null);

        IrisGstGatewayAdapter adapter = new IrisGstGatewayAdapter(config);

        CustomException ce = assertThrows(CustomException.class, () -> adapter.searchGstin("03AAAPS0001B1ZW"));
        assertEquals(GstConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("gst.verification.api.key is not configured"));
    }
}
