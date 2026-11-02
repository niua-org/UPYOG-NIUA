package org.egov.user.domain.service.multiauth;

import lombok.extern.slf4j.Slf4j;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.service.multiauth.strategy.MultiAuthStrategy;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory and registry component providing dynamic strategy resolution for multi-auth channels.
 *
 * <p>Enforces the Open/Closed Principle (OCP): new strategies are auto-discovered via Spring DI
 * without modifying this factory or calling services.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class MultiAuthStrategyFactory {

    private final List<MultiAuthStrategy> strategies;

    @Autowired
    public MultiAuthStrategyFactory(List<MultiAuthStrategy> strategies) {
        this.strategies = strategies;
        log.info("[MULTI-AUTH-FACTORY] Initialized MultiAuthStrategyFactory with {} registered strategies",
                strategies != null ? strategies.size() : 0);
    }

    /**
     * Resolves the strategy dynamically by inspecting the request's explicit authType or present attributes.
     *
     * @param request the incoming loginv2 request
     * @return matching {@link MultiAuthStrategy}
     * @throws CustomException if no strategy matches
     */
    public MultiAuthStrategy getStrategy(LoginV2Request request) {
        if (request == null) {
            log.error("[MULTI-AUTH-FACTORY] Request cannot be null for strategy resolution");
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, UserConstants.MSG_LOGIN_REQUEST_NULL);
        }

        if (request.getAuthType() != null) {
            log.debug("[MULTI-AUTH-FACTORY] Resolving strategy by explicit authType: {}", request.getAuthType());
            return getStrategy(request.getAuthType());
        }

        log.debug("[MULTI-AUTH-FACTORY] Attempting dynamic strategy resolution by request attributes (canHandle)");
        return strategies.stream()
                .filter(strategy -> strategy.canHandle(request))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("[MULTI-AUTH-FACTORY] No strategy found capable of handling request: {}", request);
                    return new CustomException(UserConstants.ERROR_UNSUPPORTED_AUTH_TYPE,
                            UserConstants.MSG_UNSUPPORTED_AUTH_CREDENTIALS);
                });
    }

    /**
     * Resolves the appropriate strategy instance corresponding to the requested {@link AuthType}.
     *
     * @param authType target authentication type
     * @return matching {@link MultiAuthStrategy}
     * @throws CustomException if no strategy is registered for the authType
     */
    public MultiAuthStrategy getStrategy(AuthType authType) {
        if (authType == null) {
            log.error("[MULTI-AUTH-FACTORY] authType cannot be null");
            throw new CustomException(UserConstants.ERROR_AUTH_TYPE_REQUIRED, UserConstants.MSG_AUTH_TYPE_NULL);
        }
        return strategies.stream()
                .filter(strategy -> strategy.supports(authType))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("[MULTI-AUTH-FACTORY] No strategy found for channel: {}", authType);
                    return new CustomException(UserConstants.ERROR_UNSUPPORTED_AUTH_TYPE,
                            UserConstants.MSG_UNSUPPORTED_AUTH_CHANNEL + authType);
                });
    }
}
