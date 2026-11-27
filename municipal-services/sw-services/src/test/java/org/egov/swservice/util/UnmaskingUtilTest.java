package org.egov.swservice.util;

import org.egov.common.contract.request.PlainAccessRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.swservice.service.UserService;
import org.egov.swservice.web.models.OwnerInfo;
import org.egov.swservice.web.models.PlumberInfo;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.users.UserDetailResponse;
import org.egov.swservice.web.models.users.UserSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnmaskingUtilTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UnmaskingUtil unmaskingUtil;

    @Test
    void getAllFieldsPlainAccessList_returnsExpectedFields() {
        assertThat(UnmaskingUtil.getAllFieldsPlainAccessList())
                .contains("mobileNumber", "name", "gender");
    }

    @Test
    void getUnmaskedPlumberInfo_whenMaskedMobile_replacesFromSearchResult() {
        PlumberInfo masked = new PlumberInfo();
        masked.setId("pl-1");
        masked.setMobileNumber("99****99");
        PlumberInfo unmasked = new PlumberInfo();
        unmasked.setId("pl-1");
        unmasked.setMobileNumber("9911223344");

        boolean swapped = unmaskingUtil.getUnmaskedPlumberInfo(List.of(masked), List.of(unmasked));

        assertThat(swapped).isTrue();
        assertThat(masked.getMobileNumber()).isEqualTo("9911223344");
    }

    @Test
    void getOwnerDetailsUnmasked_whenUserReturned_unmasksMaskedFields() {
        OwnerInfo holder = new OwnerInfo();
        holder.setUuid("u1");
        holder.setMobileNumber("99****99");
        holder.setName("Jo**");
        holder.setUserName("user");
        holder.setGender("M");
        OwnerInfo full = new OwnerInfo();
        full.setUuid("u1");
        full.setMobileNumber("9911223344");
        full.setName("John");
        full.setUserName("user");
        full.setGender("M");
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setTenantId("pb.amritsar");
        connection.setConnectionHolders(List.of(holder));
        RequestInfo requestInfo = RequestInfo.builder()
                .plainAccessRequest(PlainAccessRequest.builder().recordId("u1").build())
                .build();
        when(userService.getBaseUserSearchRequest(any(), any())).thenReturn(new UserSearchRequest());
        UserDetailResponse response = new UserDetailResponse();
        response.setUser(List.of(full));
        when(userService.getUser(any(UserSearchRequest.class))).thenReturn(response);

        unmaskingUtil.getOwnerDetailsUnmasked(connection, requestInfo);

        assertThat(holder.getMobileNumber()).isEqualTo("9911223344");
        assertThat(holder.getName()).isEqualTo("John");
    }
}
