package org.egov.user.domain.service.multiauth;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.service.multiauth.strategy.AuthenticatedUserResult;
import org.egov.user.domain.service.multiauth.strategy.MultiAuthStrategy;
import org.egov.user.domain.service.multiauth.validator.LoginV2Validator;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.egov.user.web.contract.multiauth.LoginV2Response;
import org.egov.user.web.contract.multiauth.UserAccountSummary;
import org.egov.user.domain.service.utils.UserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrator service coordinating multi-auth strategy selection, authentication, and token generation.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Exclusively orchestrates the login workflow. Validation is delegated to {@link LoginV2Validator}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects abstractions ({@link MultiAuthStrategyFactory}, {@link TokenGenerationService}, {@link LoginV2Validator}).</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Agnostic to individual channel validation rules and credential shapes.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class LoginV2Service {

    private final MultiAuthStrategyFactory strategyFactory;
    private final TokenGenerationService tokenGenerationService;
    private final LoginV2Validator validator;
    private final UserRepository userRepository;

    @Autowired
    public LoginV2Service(MultiAuthStrategyFactory strategyFactory,
                          TokenGenerationService tokenGenerationService,
                          LoginV2Validator validator,
                          UserRepository userRepository) {
        this.strategyFactory = strategyFactory;
        this.tokenGenerationService = tokenGenerationService;
        this.validator = validator;
        this.userRepository = userRepository;
    }

    /**
     * Executes the loginv2 multi-auth orchestration flow.
     *
     * @param request the resolved loginv2 request
     * @return standard {@link LoginV2Response} containing tokens and authenticated user contract
     */
    public LoginV2Response login(LoginV2Request request) {
        log.info("[LOGIN-V2-SERVICE] Starting loginv2 orchestration for tenant: {}", request != null ? request.getTenantId() : "null");

        // 1. Delegate Validation (SRP)
        validator.validate(request);

        // 2. Resolve Strategy from Factory (OCP & DIP)
        MultiAuthStrategy strategy = strategyFactory.getStrategy(request);
        log.info("[LOGIN-V2-SERVICE] Resolved authentication strategy: {}", strategy.getClass().getSimpleName());

        // 3. Authenticate and resolve domain user
        AuthenticatedUserResult authResult = strategy.authenticateAndResolveUser(request);

        // 3.1 Handle edge case: multiple citizen profiles linked to the same mobile number
        if (authResult != null && authResult.isMultipleUsersFound()) {
            log.info("[LOGIN-V2-SERVICE] Multiple users found ({}) for mobile authentication. Returning user accounts for selection.",
                    authResult.getMatchedUsers() != null ? authResult.getMatchedUsers().size() : 0);

            List<UserAccountSummary> summaries = authResult.getMatchedUsers() != null
                    ? authResult.getMatchedUsers().stream()
                        .map(UserAccountSummary::fromUser)
                        .collect(Collectors.toList())
                    : Collections.emptyList();

            ResponseInfo responseInfo = ResponseInfo.builder()
                    .status(String.valueOf(HttpStatus.OK.value()))
                    .apiId(UserConstants.API_ID_LOGIN_V2)
                    .ver(UserConstants.VERSION_2_0)
                    .ts(System.currentTimeMillis())
                    .msgId(UUID.randomUUID().toString())
                    .resMsgId(UserConstants.RES_MSG_ID_MULTIPLE_USERS)
                    .build();

            return LoginV2Response.builder()
                    .responseInfo(responseInfo)
                    .multipleUsersFound(true)
                    .userAccounts(summaries)
                    .authenticatedVia(authResult.getAuthType())
                    .build();
        }

        if (authResult == null || authResult.getUser() == null) {
            log.error("[LOGIN-V2-SERVICE] Authentication failed: strategy returned null user");
            throw new CustomException(UserConstants.ERROR_AUTHENTICATION_FAILED, UserConstants.MSG_AUTH_FAILED_NULL_USER);
        }

        log.info("[LOGIN-V2-SERVICE] Strategy completed successfully. User ID: {}, newAccountProvisioned: {}, channel: {}",
                authResult.getUser().getId(), authResult.isNewUser(), authResult.getAuthType());

        // 3.5 Update and persist last login method
        String loginMethod = authResult.getAuthType() != null ? authResult.getAuthType().name() : "MOBILE";
        authResult.getUser().setLastLoginMethod(loginMethod);
        if (authResult.getUser().getId() != null) {
            userRepository.updateLastLoginMethod(authResult.getUser().getId(), loginMethod);
        }

        // 4. Issue tokens and session metadata (SRP)
        LoginV2Response response = tokenGenerationService.generateTokensAndResponse(
                authResult.getUser(),
                request.getScope(),
                authResult.getAuthType(),
                authResult.isNewUser()
        );

        log.info("[LOGIN-V2-SERVICE] Successfully completed loginv2 orchestration for user ID: {}", authResult.getUser().getId());
        return response;
    }
}
