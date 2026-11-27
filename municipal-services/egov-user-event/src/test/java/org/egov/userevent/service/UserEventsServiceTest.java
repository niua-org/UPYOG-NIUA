package org.egov.userevent.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.userevent.repository.UserEventRepository;
import org.egov.userevent.utils.ResponseInfoFactory;
import org.egov.userevent.web.contract.EventSearchCriteria;
import org.egov.userevent.web.validator.UserEventsValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEventsServiceTest {

    @Mock private UserEventsValidator validator;
    @Mock private UserEventRepository repository;
    @Mock private ResponseInfoFactory responseInfo;

    @InjectMocks
    private UserEventsService userEventsService;

    @Test
    void searchEvents_whenUpdateMode_returnsRepositoryResults() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().type("CITIZEN").build())
                .build();
        EventSearchCriteria criteria = new EventSearchCriteria();
        criteria.setTenantId("pb.amritsar");
        when(repository.fetchEvents(criteria)).thenReturn(Collections.emptyList());
        when(repository.fetchTotalEventCount(criteria)).thenReturn(0);
        when(responseInfo.createResponseInfoFromRequestInfo(any(), eq(true)))
                .thenReturn(ResponseInfo.builder().status("successful").build());

        assertThat(userEventsService.searchEvents(requestInfo, criteria, true).getEvents()).isEmpty();
    }
}
