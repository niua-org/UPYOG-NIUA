package org.egov.user.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.Error;
import org.egov.common.contract.response.ErrorResponse;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.TokenWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "Session Logout", description = "Endpoint for revoking active OAuth2 access tokens and terminating user sessions")
@RestController
@Slf4j
public class LogoutController {

    // CHANGED: TokenStore -> OAuth2AuthorizationService
    private OAuth2AuthorizationService authorizationService;

    // UPDATED CONSTRUCTOR
    public LogoutController(OAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * End-point to logout the session.
     * Updated to work with OAuth2AuthorizationService
     *
     * @param tokenWrapper containing the access token
     * @return ResponseInfo indicating success or failure
     * @throws Exception
     */
    @Operation(
            summary = "Logout User Session",
            description = "Revokes the provided OAuth2 access token and removes the associated authorization session."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session logged out or token already expired",
                    content = @Content(schema = @Schema(implementation = ResponseInfo.class))),
            @ApiResponse(responseCode = "400", description = "Access token missing or logout failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/_logout")
    public ResponseInfo deleteToken(@RequestBody TokenWrapper tokenWrapper) throws Exception {
        String accessToken = tokenWrapper.getAccessToken();
        
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Access token is required");
        }
        
        // UPDATED: Find authorization by access token
        OAuth2Authorization authorization = authorizationService.findByToken(accessToken, OAuth2TokenType.ACCESS_TOKEN);
        
        if (authorization != null) {
            // UPDATED: Remove the entire authorization (which includes all associated tokens)
            authorizationService.remove(authorization);
            return new ResponseInfo("", "", System.currentTimeMillis(), "", "", "Logout successfully");
        } else {
            // Token not found or already expired
            return new ResponseInfo("", "", System.currentTimeMillis(), "", "", "Token not found or already expired");
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleError(Exception ex) {
        log.error("Logout request encountered an error: {}", ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse();
        ResponseInfo responseInfo = new ResponseInfo("", "", System.currentTimeMillis(), "", "", "Logout failed");
        response.setResponseInfo(responseInfo);
        Error error = new Error();
        error.setCode(400);
        error.setDescription("Logout failed: " + ex.getMessage());
        response.setError(error);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
