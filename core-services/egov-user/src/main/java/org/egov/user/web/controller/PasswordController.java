package org.egov.user.web.controller;

import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.service.UserService;
import org.egov.user.web.contract.LoggedInUserUpdatePasswordRequest;
import org.egov.user.web.contract.NonLoggedInUserUpdatePasswordRequest;
import org.egov.user.web.contract.UpdatePasswordResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Password Management", description = "Endpoints for updating passwords for logged-in and non-logged-in users")
@RestController
@RequestMapping("password")
@Slf4j
public class PasswordController {

    private UserService userService;

    public PasswordController(UserService userService) {
        this.userService = userService;
    }

    /**
     * end-point to update the password for loggedInUser
     *
     * @param request
     * @return
     */
    @Operation(
            summary = "Update Password (Logged-in User)",
            description = "Updates the password for an authenticated user after validating the existing password."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully",
                    content = @Content(schema = @Schema(implementation = UpdatePasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid password update request or password mismatch"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_update")
    public UpdatePasswordResponse updatePassword(@RequestBody @Valid LoggedInUserUpdatePasswordRequest request) {
        userService.updatePasswordForLoggedInUser(request.toDomain());
        return new UpdatePasswordResponse(ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build());
    }

    /**
     * end-point to update the password for non logged in user
     *
     * @param request
     * @return
     */
    @Operation(
            summary = "Update Password (Non-Logged-in / Forgot Password)",
            description = "Resets the password for a non-logged-in user after validating OTP."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = UpdatePasswordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid OTP or password reset request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/nologin/_update")
    public UpdatePasswordResponse updatePasswordForNonLoggedInUser(@RequestBody @Valid NonLoggedInUserUpdatePasswordRequest request) {
        userService.updatePasswordForNonLoggedInUser(request.toDomain(), request.getRequestInfo());

        return new UpdatePasswordResponse(ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build());
    }
}
