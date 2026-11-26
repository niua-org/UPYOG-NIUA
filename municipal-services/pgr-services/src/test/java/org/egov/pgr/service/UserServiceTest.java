package org.egov.pgr.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pgr.config.PGRConfiguration;
import org.egov.pgr.util.UserUtils;
import org.egov.pgr.web.models.RequestSearchCriteria;
import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.ServiceRequest;
import org.egov.pgr.web.models.ServiceWrapper;
import org.egov.pgr.web.models.User;
import org.egov.pgr.web.models.user.UserDetailResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock private UserUtils userUtils;
    @Mock private PGRConfiguration config;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void stubUserEndpoints() {
        when(config.getUserHost()).thenReturn("http://user");
        when(config.getUserContextPath()).thenReturn("/user");
        when(config.getUserCreateEndpoint()).thenReturn("/create");
        when(config.getUserUpdateEndpoint()).thenReturn("/update");
        when(config.getUserSearchEndpoint()).thenReturn("/search");
        when(userUtils.getStateLevelTenant("pg.citya")).thenReturn("pg");
    }

    @Test
    void callUserService_whenAccountIdPresent_enrichesCitizenFromSearch() {
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .service(Service.builder().tenantId("pg.citya").accountId("acc-1").build())
                .build();
        User citizen = User.builder().uuid("acc-1").name("Citizen").build();
        UserDetailResponse response = new UserDetailResponse(null, List.of(citizen));
        when(userUtils.userCall(any(), any())).thenReturn(response);

        userService.callUserService(request);

        assertThat(request.getService().getCitizen()).isEqualTo(citizen);
    }

    @Test
    void callUserService_whenAccountIdMissing_createsUserFromCitizenPayload() {
        User incoming = User.builder().mobileNumber("9876543210").name("New").emailId("a@b.com").build();
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .service(Service.builder().tenantId("pg.citya").citizen(incoming).build())
                .build();
        UserDetailResponse emptySearch = new UserDetailResponse(null, Collections.emptyList());
        User created = User.builder().uuid("new-uuid").name("New").emailId("a@b.com").build();
        UserDetailResponse createResponse = new UserDetailResponse(null, List.of(created));
        when(userUtils.userCall(any(), any())).thenReturn(emptySearch, createResponse);

        userService.callUserService(request);

        assertThat(request.getService().getAccountId()).isEqualTo("new-uuid");
    }

    @Test
    void enrichUsers_mapsCitizensOntoServiceWrappers() {
        ServiceWrapper wrapper = ServiceWrapper.builder()
                .service(Service.builder().accountId("u1").build())
                .build();
        User user = User.builder().uuid("u1").name("Citizen").build();
        UserDetailResponse response = new UserDetailResponse(null, List.of(user));
        when(userUtils.userCall(any(), any())).thenReturn(response);

        userService.enrichUsers(List.of(wrapper), RequestInfo.builder().build(), "pg.citya");

        assertThat(wrapper.getService().getCitizen()).isEqualTo(user);
    }

    @Test
    void enrichUserIds_setsUserIdsFromMobileSearch() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .mobileNumber("9876543210")
                .build();
        User user = User.builder().uuid("uuid-1").build();
        UserDetailResponse response = new UserDetailResponse(null, List.of(user));
        when(userUtils.userCall(any(), any())).thenReturn(response);

        userService.enrichUserIds("pg.citya", criteria);

        assertThat(criteria.getUserIds()).isEqualTo(Set.of("uuid-1"));
    }

    @Test
    void callUserService_whenAccountIdUnknown_throwsInvalidAccountId() {
        ServiceRequest request = ServiceRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .service(Service.builder().tenantId("pg.citya").accountId("missing").build())
                .build();
        UserDetailResponse response = new UserDetailResponse(null, Collections.emptyList());
        when(userUtils.userCall(any(), any())).thenReturn(response);

        assertThatThrownBy(() -> userService.callUserService(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("No user exist");
    }
}
