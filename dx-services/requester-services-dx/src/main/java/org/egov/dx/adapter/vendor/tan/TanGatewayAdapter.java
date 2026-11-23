package org.egov.dx.adapter.vendor.tan;

import org.egov.dx.adapter.vendor.dto.VendorTanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationResponse;

/**
 * Gateway Adapter interface for communicating with Income Tax Department (ITD)
 * TAN verification web services or mock simulators.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Dependency Inversion Principle (DIP):</b> High-level controllers and services depend on this abstraction, not concrete implementations.</li>
 *   <li><b>Interface Segregation Principle (ISP):</b> Isolates operations strictly needed for TAN verification.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> New providers or environments can be plugged in without changing client code.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface TanGatewayAdapter {

    /**
     * Verifies the given TAN with the Income Tax Department database or mock provider.
     *
     * @param request {@link VendorTanVerificationRequest} containing tracking header and TAN search criteria
     * @return {@link VendorTanVerificationResponse} containing verified deductor details, category, and jurisdiction
     */
    VendorTanVerificationResponse verifyTan(VendorTanVerificationRequest request);
}
