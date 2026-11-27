package org.egov.dx.service;

import org.egov.dx.web.models.GstProfileRequest;
import org.egov.dx.web.models.GstProfileResponse;

/**
 * Service contract for GST taxpayer profile search and verification operations.
 *
 * <p>Enforces the <b>Dependency Inversion Principle (DIP)</b> by providing a clean domain abstraction
 * for controllers, isolating all vendor translation, validation delegation, and response enrichment.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface GstProfileService {

    /**
     * Orchestrates validation, gateway invocation, and domain response enrichment for GST search.
     *
     * @param request {@link GstProfileRequest} containing RequestInfo and 15-character GSTIN
     * @return {@link GstProfileResponse} containing verified taxpayer profile and registration status
     */
    GstProfileResponse searchGstin(GstProfileRequest request);
}
