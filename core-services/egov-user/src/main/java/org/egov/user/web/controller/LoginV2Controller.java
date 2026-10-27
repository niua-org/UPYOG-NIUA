package org.egov.user.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.user.domain.service.multiauth.LoginV2Service;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.egov.user.web.contract.multiauth.LoginV2Response;
import org.egov.user.web.controller.resolver.LoginV2RequestResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing the unified multi-channel citizen authentication endpoint under {@code /users/v2}.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles HTTP transport and dispatching.
 *       Request parameter resolution/fallback is delegated to {@link LoginV2RequestResolver},
 *       while orchestration is handled by {@link LoginV2Service}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> All dependencies are injected abstractions.</li>
 * </ul>
 *
 * <p>Endpoint: {@code POST /users/v2/_login}</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Tag(name = "Authentication V2", description = "Multi-channel unified citizen authentication (Mobile OTP, UIDAI Aadhaar e-KYC, and Family ID)")
@RestController
@RequestMapping("/users/v2")
@Slf4j
public class LoginV2Controller {

    private final LoginV2Service loginV2Service;
    private final LoginV2RequestResolver requestResolver;

    @Autowired
    public LoginV2Controller(LoginV2Service loginV2Service,
                             LoginV2RequestResolver requestResolver) {
        this.loginV2Service = loginV2Service;
        this.requestResolver = requestResolver;
    }

    /**
     * Unified multi-auth entry point supporting Mobile, Aadhaar, and Family ID channels.
     * Mapped to canonical {@code POST /users/v2/_login}.
     *
     * @param requestBody optional JSON body binding {@link LoginV2Request}
     * @param servletRequest underlying servlet request for query/form parameter extraction
     * @return {@link ResponseEntity} wrapping {@link LoginV2Response}
     */
    @Operation(
            summary = "Citizen Multi-Auth Login (V2)",
            description = "Unified authentication endpoint supporting Mobile OTP, UIDAI Aadhaar e-KYC, and Family ID channels. Issues an OAuth2 bearer access token upon successful authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful, token and user profile returned",
                    content = @Content(schema = @Schema(implementation = LoginV2Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or malformed credentials"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - OTP or Aadhaar verification failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_login")
    public ResponseEntity<LoginV2Response> login(
            @RequestBody(required = false) @Valid LoginV2Request requestBody,
            HttpServletRequest servletRequest) {

        String clientIp = requestResolver.resolveClientIp(servletRequest);
        log.info("[LOGIN-V2-CONTROLLER] Received login request at POST /users/v2/_login from IP: {}", clientIp);

        // 1. Resolve and enrich request payload (SRP)
        LoginV2Request effectiveRequest = requestResolver.resolve(requestBody, servletRequest);

        // 2. Dispatch to orchestrator service
        LoginV2Response response = loginV2Service.login(effectiveRequest);

        log.info("[LOGIN-V2-CONTROLLER] Completed login request with HTTP 200 OK for channel: {}",
                response.getAuthenticatedVia());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
