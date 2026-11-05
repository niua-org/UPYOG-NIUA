package org.egov.user.web.contract;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.enums.UserType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUserRequestScopeTest {

    @Test
    void toDomain_withScope_setsScopeOnDomainUser() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder().id(1L).uuid("actor-uuid").build())
                .build();
        UserRequest userRequest = UserRequest.builder()
                .uuid("uuid-1")
                .userName("9999999999")
                .name("Citizen")
                .type(UserType.CITIZEN)
                .tenantId("pb")
                .active(true)
                .roles(Set.of())
                .build();

        CreateUserRequest request = new CreateUserRequest(requestInfo, userRequest, "profile-update");

        assertThat(request.toDomain(false).getScope()).isEqualTo("profile-update");
    }

    @Test
    void toDomain_blankScope_leavesScopeUnset() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(org.egov.common.contract.request.User.builder().id(1L).uuid("actor-uuid").build())
                .build();
        UserRequest userRequest = UserRequest.builder()
                .uuid("uuid-2")
                .userName("8888888888")
                .name("Citizen")
                .type(UserType.CITIZEN)
                .tenantId("pb")
                .active(true)
                .roles(Set.of())
                .build();

        CreateUserRequest request = new CreateUserRequest(requestInfo, userRequest, "  ");

        assertThat(request.toDomain(true).getScope()).isNull();
    }
}
