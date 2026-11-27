package org.egov.pt.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.producer.PropertyProducer;
import org.egov.pt.repository.ServiceRequestRepository;
import org.egov.pt.service.UserService;
import org.egov.pt.web.contracts.SMSRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationUtilTest {

    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private PropertyConfiguration config;
    @Mock private PropertyProducer producer;
    @Mock private RestTemplate restTemplate;
    @Mock private UserService userService;

    private NotificationUtil notificationUtil;

    @BeforeEach
    void createUtil() {
        notificationUtil = new NotificationUtil(serviceRequestRepository, config, producer, restTemplate, userService);
    }

    @Test
    void getMessageTemplate_whenCodePresent_returnsLocalizedMessage() {
        String json = "{\"messages\":[{\"code\":\"PT_TEST\",\"message\":\"Hello {OWNER}\"}]}";

        String msg = notificationUtil.getMessageTemplate("PT_TEST", json);

        assertThat(msg).isEqualTo("Hello {OWNER}");
    }

    @Test
    void getMessageTemplate_whenCodeMissing_returnsEmptyString() {
        assertThat(notificationUtil.getMessageTemplate("MISSING", "{}")).isEmpty();
    }

    @Test
    void getUri_whenStateLevelLocalization_usesStateTenant() {
        when(config.getIsLocalizationStateLevel()).thenReturn(true);
        when(config.getLocalizationHost()).thenReturn("http://loc");
        when(config.getLocalizationContextPath()).thenReturn("/loc");
        when(config.getLocalizationSearchEndpoint()).thenReturn("/search");

        StringBuilder uri = notificationUtil.getUri("pb.amritsar", RequestInfo.builder().build(), "en_IN");

        assertThat(uri.toString()).contains("tenantId=pb");
        assertThat(uri.toString()).contains("locale=en_IN");
    }

    @Test
    void createSMSRequest_whenOwnerMapGiven_replacesOwnerPlaceholder() {
        Map<String, String> owners = Map.of("9999999999", "Ravi");

        List<SMSRequest> requests = notificationUtil.createSMSRequest("Dear {OWNER_NAME}", owners);

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getMessage()).isEqualTo("Dear Ravi");
        assertThat(requests.get(0).getMobileNumber()).isEqualTo("9999999999");
    }

    @Test
    void sendSMS_whenEnabled_pushesEachRequestToTopic() {
        when(config.getIsSMSNotificationEnabled()).thenReturn(true);
        when(config.getSmsNotifTopic()).thenReturn("sms-topic");
        List<SMSRequest> sms = List.of(new SMSRequest("999", "hi"));

        notificationUtil.sendSMS(sms, "pb.amritsar");

        verify(producer).push(eq(""), eq("sms-topic"), eq(sms.get(0)));
    }

}
