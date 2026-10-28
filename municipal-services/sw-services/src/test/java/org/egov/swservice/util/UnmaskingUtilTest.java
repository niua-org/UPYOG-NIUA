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
        PlumberInfo masked = PlumberInfo.builder().id("pl-1").mobileNumber("99****99").build();
        PlumberInfo unmasked = PlumberInfo.builder().id("pl-1").mobileNumber("9911223344").build();

        boolean swapped = unmaskingUtil.getUnmaskedPlumberInfo(List.of(masked), List.of(unmasked));

        assertThat(swapped).isTrue();
        assertThat(masked.getMobileNumber()).isEqualTo("9911223344");
    }

    @Test
    void getOwnerDetailsUnmasked_whenUserReturned_unmasksMaskedFields() {
        OwnerInfo holder = OwnerInfo.builder()
                .uuid("u1")
                .mobileNumber("99****99")
                .name("Jo**")
                .userName("user")
                .gender("M")
                .build();
        OwnerInfo full = OwnerInfo.builder()
                .uuid("u1")
                .mobileNumber("9911223344")
                .name("John")
                .userName("user")
                .gender("M")
                .build();
        SewerageConnection connection = SewerageConnection.builder()
                .tenantId("pb.amritsar")
                .connectionHolders(List.of(holder))
                .build();
        RequestInfo requestInfo = RequestInfo.builder()
                .plainAccessRequest(PlainAccessRequest.builder().recordId("u1").build())
                .build();
        when(userService.getBaseUserSearchRequest(any(), any())).thenReturn(UserSearchRequest.builder().build());
        when(userService.getUser(any(UserSearchRequest.class)))
                .thenReturn(UserDetailResponse.builder().user(List.of(full)).build());

        unmaskingUtil.getOwnerDetailsUnmasked(connection, requestInfo);

        assertThat(holder.getMobileNumber()).isEqualTo("9911223344");
        assertThat(holder.getName()).isEqualTo("John");
    }
}
