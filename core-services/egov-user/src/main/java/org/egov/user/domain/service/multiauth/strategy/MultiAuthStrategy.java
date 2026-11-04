package org.egov.user.domain.service.multiauth.strategy;

import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;

/**
 * Strategy interface defining authentication and user resolution behaviors for distinct channels.
 *
 * <p>Strict adherence to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Each implementation handles one specific identity channel.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> New channels (e.g. DigiLocker, Parichay) can be registered without modifying existing strategies.</li>
 *   <li><b>Liskov Substitution Principle (LSP):</b> Any strategy can be substituted cleanly by the orchestrator factory.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface MultiAuthStrategy {

    /**
     * Declares whether this strategy supports the given {@link AuthType}.
     *
     * @param authType the channel type to check
     * @return true if supported, false otherwise
     */
    boolean supports(AuthType authType);

    /**
     * Inspects whether this strategy can handle the request based on present request attributes
     * (used when authType is not explicitly supplied in the request).
     *
     * @param request the incoming loginv2 request
     * @return true if strategy matches request attributes, false otherwise
     */
    boolean canHandle(LoginV2Request request);

    /**
     * Executes OTP/credential validation for the channel, resolves or auto-creates the citizen User,
     * and returns the authenticated user result.
     *
     * @param request the incoming loginv2 request
     * @return {@link AuthenticatedUserResult} with domain user and onboarding flags
     */
    AuthenticatedUserResult authenticateAndResolveUser(LoginV2Request request);
}
