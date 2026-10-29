package org.egov.pt.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.OwnerInfo;
import org.egov.pt.models.Property;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.util.NotificationUtil;
import org.egov.pt.util.PTConstants;
import org.egov.pt.web.contracts.PropertyRequest;
import org.egov.pt.web.contracts.SMSRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private NotificationUtil notifUtil;
    @Mock private PropertyConfiguration configs;

    @InjectMocks
    private NotificationService notificationService;

    private static final String LOCAL_JSON = "{\"messages\":[{\"code\":\"PT_NOTIF_WF_UPDATE_NONE\",\"message\":\"updated\"}]}";

    @Test
    void sendNotificationForUpdate_whenWorkflowDisabled_sendsSmsToOwners() {
        OwnerInfo owner = OwnerInfo.builder().name("Owner").mobileNumber("9999999999").build();
        ProcessInstance workflow = ProcessInstance.builder().moduleName(PTConstants.PT_BUSINESSSERVICE).build();
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .propertyId("pid-1")
                .acknowldgementNumber("ACK-1")
                .creationReason(CreationReason.UPDATE)
                .workflow(workflow)
                .owners(new ArrayList<>(List.of(owner)))
                .build();
        PropertyRequest request = PropertyRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .property(property)
                .build();

        when(configs.getIsWorkflowEnabled()).thenReturn(false);
        when(notifUtil.getLocalizationMessages(anyString(), any())).thenReturn(LOCAL_JSON);
        when(notifUtil.getHost(anyString())).thenReturn("http://host");
        when(configs.getViewPropertyLink()).thenReturn("/view?pid={PROPERTY_ID}&tid={TENANT_ID}");
        when(notifUtil.getShortenedUrl(anyString())).thenReturn("http://short");
        when(notifUtil.getMessageTemplate(eq(PTConstants.UPDATE_NO_WORKFLOW), anyString())).thenReturn("updated");
        when(notifUtil.fetchChannelList(any(), anyString(), anyString(), anyString())).thenReturn(Collections.emptyList());
        when(notifUtil.createSMSRequest(anyString(), any())).thenReturn(List.of(new SMSRequest("9999999999", "updated")));

        notificationService.sendNotificationForUpdate(request);

        verify(notifUtil, atLeastOnce()).sendSMS(anyList(), eq("pb.amritsar"));
    }

    @Test
    void sendNotificationForMtPayment_whenAmountGiven_preparesAndSendsSms() {
        OwnerInfo owner = OwnerInfo.builder().name("Owner").mobileNumber("9999999999").build();
        ProcessInstance workflow = ProcessInstance.builder().moduleName(PTConstants.PT_BUSINESSSERVICE).build();
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .propertyId("pid-1")
                .acknowldgementNumber("ACK-1")
                .workflow(workflow)
                .owners(new ArrayList<>(List.of(owner)))
                .build();
        PropertyRequest request = PropertyRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .property(property)
                .build();

        when(notifUtil.getLocalizationMessages(anyString(), any())).thenReturn(LOCAL_JSON);
        when(notifUtil.getMutationUrl(any())).thenReturn("http://mut");
        when(notifUtil.getMessageTemplate(anyString(), anyString())).thenReturn("paid {AMOUNT}");
        when(notifUtil.fetchChannelList(any(), anyString(), anyString(), anyString())).thenReturn(Collections.emptyList());
        when(notifUtil.createSMSRequest(anyString(), any())).thenReturn(List.of(new SMSRequest("9999999999", "paid 100")));

        notificationService.sendNotificationForMtPayment(request, new BigDecimal("100"));

        verify(notifUtil, atLeastOnce()).sendSMS(anyList(), eq("pb.amritsar"));
    }
}
