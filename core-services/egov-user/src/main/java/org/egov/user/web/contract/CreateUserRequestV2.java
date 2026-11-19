package org.egov.user.web.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;



/**
 * Contract request payload for creating or updating a user in the V2 user schema.
 * <p>
 * Contains the caller's {@link RequestInfo} audit metadata and the {@link UserRequestV2} profile payload
 * supporting multi-address arrays. Provides {@link #toDomain(boolean)} to convert the contract into a domain {@link User}.
 */
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class CreateUserRequestV2 {
    private RequestInfo requestInfo;

    @NotNull
    @Valid
    private UserRequestV2 user;

    /**
     * Maps this V2 API request contract to a domain {@link User} entity, enriching it with the authenticated caller's identity.
     *
     * @param isCreate whether this mapping is for a user creation ({@code true}) or update ({@code false}) operation
     * @return populated domain {@link User} entity with V2 address collection
     */
    public User toDomain(boolean isCreate) {
        return user.toDomain(loggedInUserId(),loggedInUserUuid(), isCreate);
    }

    // TODO Update libraries to have uuid in request info
    private Long loggedInUserId() {
        return requestInfo.getUserInfo() == null ? null : requestInfo.getUserInfo().getId();
    }
    private String loggedInUserUuid() {
        return requestInfo.getUserInfo() == null ? null : requestInfo.getUserInfo().getUuid();
    }

}


