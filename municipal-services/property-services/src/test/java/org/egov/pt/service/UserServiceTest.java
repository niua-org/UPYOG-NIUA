package org.egov.pt.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.models.user.UserSearchRequest;
import org.egov.pt.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private com.fasterxml.jackson.databind.ObjectMapper mapper;
    @Mock private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUserServiceEndpoints() {
        ReflectionTestUtils.setField(userService, "userHost", "http://user");
        ReflectionTestUtils.setField(userService, "userContextPath", "/user");
        ReflectionTestUtils.setField(userService, "userCreateEndpoint", "/create");
        ReflectionTestUtils.setField(userService, "userSearchEndpoint", "/search");
        ReflectionTestUtils.setField(userService, "userUpdateEndpoint", "/update");
    }

    @Test
    void getBaseUserSearchRequest_whenTenantGiven_buildsCitizenSearch() {
        RequestInfo requestInfo = RequestInfo.builder().build();

        UserSearchRequest search = userService.getBaseUserSearchRequest("pb.amritsar", requestInfo);

        assertThat(search.getTenantId()).isEqualTo("pb.amritsar");
        assertThat(search.getUserType()).isEqualTo("CITIZEN");
        assertThat(search.getActive()).isTrue();
        assertThat(search.getRequestInfo()).isSameAs(requestInfo);
    }
}
