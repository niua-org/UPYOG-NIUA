package org.egov.user.domain.service.multiauth.validator;

import org.egov.user.web.contract.multiauth.LoginV2Request;

/**
 * Validator interface establishing request constraint contracts for multi-auth login operations.
 *
 * <p>Enforces the Single Responsibility Principle (SRP) by isolating validation rules away
 * from controllers and orchestrator services.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface LoginV2Validator {

    /**
     * Validates structural and channel-specific invariants on the incoming {@link LoginV2Request}.
     *
     * @param request the resolved login request
     * @throws org.egov.tracer.model.CustomException if validation fails
     */
    void validate(LoginV2Request request);
}
