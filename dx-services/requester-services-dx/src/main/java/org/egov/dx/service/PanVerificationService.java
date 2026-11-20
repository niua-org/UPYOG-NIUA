package org.egov.dx.service;

import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.dx.web.models.PanVerificationResponse;

/**
 * Service contract for PAN verification business operations.
 *
 * <p>Enforces the <b>Dependency Inversion Principle (DIP)</b> by providing a clean domain abstraction
 * for controllers, isolating all vendor translation, validation delegation, and response enrichment.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface PanVerificationService {

    /**
     * Orchestrates validation, vendor translation, gateway invocation, and response enrichment for PAN verification.
     *
     * @param request {@link PanVerificationRequest} containing RequestInfo, PAN, fullName, and DOB
     * @return {@link PanVerificationResponse} containing verified taxpayer details and match status
     */
    PanVerificationResponse verifyPan(PanVerificationRequest request);
}
