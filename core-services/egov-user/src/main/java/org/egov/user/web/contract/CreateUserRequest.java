package org.egov.user.web.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class CreateUserRequest {
    private RequestInfo requestInfo;

    @NotNull
    @Valid
    private UserRequest user;

    private String scope;

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


