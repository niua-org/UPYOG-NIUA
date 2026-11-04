package org.egov.user.web.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Contract request payload for creating or updating a user in the legacy (V1) user schema.
 * <p>
 * Contains the caller's {@link RequestInfo} audit metadata and the {@link UserRequest} profile payload.
 * Provides {@link #toDomain(boolean)} to convert the incoming web contract into a validated domain {@link User} entity.
 */
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class CreateUserRequest {
    private RequestInfo requestInfo;

    @NotNull
    @Valid
    private UserRequest user;

    private String scope;

    /**
     * Maps this API request contract to a domain {@link User} entity, enriching it with the authenticated caller's identity.
     *
     * @param isCreate whether this mapping is for a user creation ({@code true}) or update ({@code false}) operation
     * @return populated domain {@link User} entity
     */
    public User toDomain(boolean isCreate) {
        User domainUser = user.toDomain(loggedInUserId(), loggedInUserUuid(), isCreate);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(this.scope)) {
            domainUser.setScope(this.scope);
        }
        return domainUser;
    }

    // TODO Update libraries to have uuid in request info
    private Long loggedInUserId() {
        return requestInfo.getUserInfo() == null ? null : requestInfo.getUserInfo().getId();
    }
    private String loggedInUserUuid() {
        return requestInfo.getUserInfo() == null ? null : requestInfo.getUserInfo().getUuid();
    }

}


