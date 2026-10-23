package org.egov.dx.adapter.vendor;

import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;

/**
 * Gateway Adapter interface for communicating with third-party Aadhaar KYC and OTP providers.
 *
 * <p>Adheres strictly to SOLID principles:</p>
 * <ul>
 *   <li><b>Dependency Inversion Principle (DIP):</b> High-level controllers depend on this abstraction, not concrete implementations.</li>
 *   <li><b>Interface Segregation Principle (ISP):</b> Segregates only the exact operations required for Aadhaar verification.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> New providers (e.g. Protean, NSDL, UIDAI direct) can be integrated by creating new adapter implementations without altering existing code.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface AadhaarGatewayAdapter {

    /**
     * Dispatches an OTP to the citizen's Aadhaar-linked mobile number.
     *
     * @param request the vendor send OTP request containing Aadhaar number and consent
     * @return {@link VendorSendOtpResponse} containing transaction ID, reference ID, and status
     */
    VendorSendOtpResponse sendOtp(VendorSendOtpRequest request);

    /**
     * Verifies the citizen-submitted OTP against the active session reference ID.
     *
     * @param request the vendor verify OTP request containing session reference ID and OTP
     * @return {@link VendorVerifyOtpSuccessResponse} containing verified user demographics and address
     */
    VendorVerifyOtpSuccessResponse verifyOtp(VendorVerifyOtpRequest request);
}
