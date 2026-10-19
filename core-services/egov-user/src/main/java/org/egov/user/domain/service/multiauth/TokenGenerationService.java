package org.egov.user.domain.service.multiauth;

import org.egov.user.domain.model.User;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Response;

/**
 * Service abstraction responsible for issuing authentication tokens (OAuth2/JWT) and session storage.
 *
 * <p>Adheres strictly to the Single Responsibility Principle (SRP) by decoupling token minting
 * and session serialization from multi-channel user authentication strategies.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface TokenGenerationService {

    /**
     * Issues access token and refresh token, commits the session metadata to Redis,
     * and constructs the unified {@link LoginV2Response}.
     *
     * @param user the authenticated domain user
     * @param scope token scope requested (defaults to "read write")
     * @param authType identity channel used
     * @param isNewUser whether this citizen profile was newly provisioned during login
     * @return standard {@link LoginV2Response} containing access token, expiration, and user contract
     */
    LoginV2Response generateTokensAndResponse(User user, String scope, AuthType authType, boolean isNewUser);
}
