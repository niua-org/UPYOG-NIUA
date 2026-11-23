package org.egov.dx.service;

import org.egov.dx.web.models.TanVerificationRequest;
import org.egov.dx.web.models.TanVerificationResponse;

/**
 * Service contract for TAN verification business operations.
 *
 * <p>Enforces the <b>Dependency Inversion Principle (DIP)</b> by providing a clean domain abstraction
 * for controllers, isolating all vendor translation, validation delegation, and response enrichment.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface TanVerificationService {

    /**
     * Orchestrates validation, vendor translation, gateway invocation, and response enrichment for TAN verification.
     *
     * @param request {@link TanVerificationRequest} containing RequestInfo and TAN
     * @return {@link TanVerificationResponse} containing verified deductor details and status
     */
    TanVerificationResponse verifyTan(TanVerificationRequest request);
}
