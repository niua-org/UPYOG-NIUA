package org.egov.dx.adapter.vendor.pan;

import org.egov.dx.adapter.vendor.dto.VendorPanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationResponse;

/**
 * Gateway Adapter interface for communicating with Income Tax Department (ITD)
 * PAN verification web services or mock simulators.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Dependency Inversion Principle (DIP):</b> High-level services depend on this abstraction, not concrete implementations.</li>
 *   <li><b>Interface Segregation Principle (ISP):</b> Isolates operations strictly needed for PAN verification.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> New providers (Protean, NSDL, ITD) can be plugged in without changing domain code.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface PanGatewayAdapter {

    /**
     * Verifies the given PAN with the Income Tax Department database or mock provider.
     *
     * @param request {@link VendorPanVerificationRequest} containing tracking header and search parameters
     * @return {@link VendorPanVerificationResponse} containing verified PAN status, demographic matches, and seeding status
     */
    VendorPanVerificationResponse verifyPan(VendorPanVerificationRequest request);
}
