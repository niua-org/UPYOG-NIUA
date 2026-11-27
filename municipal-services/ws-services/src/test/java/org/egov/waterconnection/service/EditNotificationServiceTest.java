package org.egov.waterconnection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.util.NotificationUtil;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.egov.waterconnection.validator.ValidateProperty;
import org.egov.waterconnection.web.models.OwnerInfo;
import org.egov.waterconnection.web.models.Property;
import org.egov.waterconnection.web.models.SMSRequest;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.egov.waterconnection.web.models.users.UserDetailResponse;
import org.egov.waterconnection.web.models.workflow.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditNotificationServiceTest {

    @Mock private WSConfiguration config;
    @Mock private NotificationUtil notificationUtil;
    @Mock private ObjectMapper mapper;
    @Mock private WorkflowNotificationService workflowNotificationService;
    @Mock private ValidateProperty validateProperty;
    @Mock private WaterServicesUtil waterServicesUtil;

    @InjectMocks
    private EditNotificationService editNotificationService;

    @Test
    void sendEditNotification_whenSmsChannelEnabled_sendsSmsToOwners() {
        User applicant = User.builder().uuid("app-1").mobileNumber("9999999999").name("Applicant").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(applicant).build();
        ProcessInstance processInstance = ProcessInstance.builder().action(WCConstants.ACTION_INITIATE).build();
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setTenantId("pb.amritsar");
        waterConnection.setApplicationStatus(WCConstants.STATUS_INITIATED);
        waterConnection.setProcessInstance(processInstance);
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(requestInfo)
                .waterConnection(waterConnection)
                .build();
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .owners(List.of(OwnerInfo.builder().uuid("owner-1").mobileNumber("8888888888").name("Owner").build()))
                .build();

        when(notificationUtil.fetchChannelList(any(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(WCConstants.CHANNEL_NAME_SMS));
        when(config.getIsSMSEnabled()).thenReturn(true);
        when(notificationUtil.getInternalMicroserviceUser(anyString())).thenReturn(User.builder().uuid("internal").build());
        when(validateProperty.getOrValidateProperty(request)).thenReturn(property);
        when(notificationUtil.getLocalizationMessages(anyString(), any())).thenReturn("{}");
        when(notificationUtil.getCustomizedMsg(anyString(), anyString())).thenReturn("Edit message");
        when(waterServicesUtil.isModifyConnectionRequestForNotification(request)).thenReturn(false);
        when(waterServicesUtil.isDisconnectConnectionRequest(request)).thenReturn(false);
        UserDetailResponse userDetailResponse = new UserDetailResponse();
        userDetailResponse.setUser(List.of(OwnerInfo.builder().mobileNumber("8888888888").name("Owner").build()));
        when(workflowNotificationService.fetchUserByUUID(anySet(), any(), anyString()))
                .thenReturn(userDetailResponse);
        when(workflowNotificationService.getMessageForMobileNumber(any(), eq(request), anyString(), eq(property)))
                .thenReturn(Map.of("8888888888", "Edit message", "9999999999", "Edit message"));

        editNotificationService.sendEditNotification(request);

        verify(notificationUtil).sendSMS(anyList(), eq("pb.amritsar"));
    }

    @Test
    void sendEditNotification_whenNoChannelsConfigured_skipsNotifications() {
        WaterConnection waterConnection = new WaterConnection();
        waterConnection.setTenantId("pb.amritsar");
        waterConnection.setProcessInstance(ProcessInstance.builder().action("INITIATE").build());
        WaterConnectionRequest request = WaterConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().build()).build())
                .waterConnection(waterConnection)
                .build();
        when(notificationUtil.fetchChannelList(any(), anyString(), anyString(), anyString())).thenReturn(List.of());

        editNotificationService.sendEditNotification(request);

        verify(notificationUtil, never()).sendSMS(anyList(), anyString());
        verify(notificationUtil, never()).sendEventNotification(any(), anyString());
    }
}
