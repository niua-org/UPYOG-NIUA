package org.egov.dx.adapter.vendor;

import lombok.extern.slf4j.Slf4j;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.springframework.stereotype.Service;

/**
 * Service facade delegating to the active {@link AadhaarGatewayAdapter}.
 *
 * <p>Satisfies the <b>Dependency Inversion Principle (DIP)</b> and <b>Open/Closed Principle (OCP)</b>:
 * Delegates operations to the polymorphic adapter injected by Spring according to the active configuration.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class AadhaarGatewayService {

    private final AadhaarGatewayAdapter gatewayAdapter;

    /**
     * Injects the active gateway adapter bean (either mock or live Sandbox adapter).
     *
     * @param gatewayAdapter polymorphic gateway adapter
     */
    public AadhaarGatewayService(AadhaarGatewayAdapter gatewayAdapter) {
        this.gatewayAdapter = gatewayAdapter;
    }

    /**
     * Dispatches an OTP to citizen's Aadhaar-linked mobile number via the active adapter.
     *
     * @param request the send OTP request
     * @return {@link VendorSendOtpResponse}
     */
    public VendorSendOtpResponse sendOtp(VendorSendOtpRequest request) {
        return gatewayAdapter.sendOtp(request);
    }

    /**
     * Verifies citizen OTP via the active adapter.
     *
     * @param request the verify OTP request
     * @return {@link VendorVerifyOtpSuccessResponse}
     */
    public VendorVerifyOtpSuccessResponse verifyOtp(VendorVerifyOtpRequest request) {
        return gatewayAdapter.verifyOtp(request);
    }
}
