package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.dto.VendorPanVerificationRequest;
import org.egov.dx.adapter.vendor.pan.impl.ItdPanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.PanConstants;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ItdPanGatewayAdapterTest {

    @Test
    void testConstructor_MissingConnectTimeout_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getPanVerificationConnectTimeout()).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> new ItdPanGatewayAdapter(config));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("pan.verification.connect.timeout is not configured"));
    }

    @Test
    void testConstructor_MissingReadTimeout_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getPanVerificationConnectTimeout()).thenReturn(5000);
        when(config.getPanVerificationReadTimeout()).thenReturn(null);

        CustomException ce = assertThrows(CustomException.class, () -> new ItdPanGatewayAdapter(config));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("pan.verification.read.timeout is not configured"));
    }

    @Test
    void testConstructor_MissingBaseUrl_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getPanVerificationConnectTimeout()).thenReturn(5000);
        when(config.getPanVerificationReadTimeout()).thenReturn(10000);
        when(config.getPanVerificationBaseUrl()).thenReturn("");

        CustomException ce = assertThrows(CustomException.class, () -> new ItdPanGatewayAdapter(config));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("pan.verification.base.url is not configured"));
    }

    @Test
    void testVerifyPan_MissingPath_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getPanVerificationConnectTimeout()).thenReturn(5000);
        when(config.getPanVerificationReadTimeout()).thenReturn(10000);
        when(config.getPanVerificationBaseUrl()).thenReturn("https://uatservices.incometax.gov.in/services/auth/v0.1/");
        when(config.getPanVerificationPath()).thenReturn(null);

        ItdPanGatewayAdapter adapter = new ItdPanGatewayAdapter(config);
        VendorPanVerificationRequest request = VendorPanVerificationRequest.builder().build();

        CustomException ce = assertThrows(CustomException.class, () -> adapter.verifyPan(request));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("pan.verification.path is not configured"));
    }

    @Test
    void testVerifyPan_MissingCredentials_ThrowsCustomException() {
        Configurations config = mock(Configurations.class);
        when(config.getPanVerificationConnectTimeout()).thenReturn(5000);
        when(config.getPanVerificationReadTimeout()).thenReturn(10000);
        when(config.getPanVerificationBaseUrl()).thenReturn("https://uatservices.incometax.gov.in/services/auth/v0.1/");
        when(config.getPanVerificationPath()).thenReturn("pan/verify");
        when(config.getPanVerificationClientId()).thenReturn(null);

        ItdPanGatewayAdapter adapter = new ItdPanGatewayAdapter(config);
        VendorPanVerificationRequest request = VendorPanVerificationRequest.builder().build();

        CustomException ce = assertThrows(CustomException.class, () -> adapter.verifyPan(request));
        assertEquals(PanConstants.ERR_CODE_INVALID_REQUEST, ce.getCode());
        assertTrue(ce.getMessage().contains("pan.verification.client.id is not configured"));
    }
}
