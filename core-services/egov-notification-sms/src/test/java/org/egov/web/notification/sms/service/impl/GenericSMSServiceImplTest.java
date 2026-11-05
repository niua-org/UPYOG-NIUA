package org.egov.web.notification.sms.service.impl;

import org.egov.web.notification.sms.config.SMSProperties;
import org.egov.web.notification.sms.models.Sms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericSMSServiceImplTest {

    private static final String MOBILE = "9999999999";
    private static final String GATEWAY_URL = "http://sms.example/send";

    @Mock
    private RestTemplate restTemplate;

    private GenericSMSServiceImpl smsService;

    @BeforeEach
    void setUp() {
        SMSProperties smsProperties = new SMSProperties();
        smsProperties.setUrl(GATEWAY_URL);
        smsProperties.setRequestType("POST");
        smsProperties.setContentType("application/x-www-form-urlencoded");
        smsProperties.setMobileNumberPrefix("");
        smsProperties.setBlacklistNumbers(Collections.<String>emptyList());
        smsProperties.setWhitelistNumbers(Collections.<String>emptyList());

        Map<String, String> configMap = new LinkedHashMap<String, String>();
        configMap.put("mobileno", "$mobileno");
        configMap.put("message", "$message");
        smsProperties.setConfigMap(configMap);

        smsService = new GenericSMSServiceImpl();
        ReflectionTestUtils.setField(smsService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(smsService, "smsProperties", smsProperties);
    }

    @Test
    void sendSMS_validMessage_submitsMessageToGateway() {
        Sms sms = new Sms(MOBILE, "OTP 1234", null, null, null);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<String>("code=API000 & info=accepted", HttpStatus.OK));

        smsService.sendSMS(sms);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> requestCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(URI.create(GATEWAY_URL)), eq(HttpMethod.POST), requestCaptor.capture(), eq(String.class));
        assertThat(requestCaptor.getValue().getBody().getFirst("mobileno")).isEqualTo(MOBILE);
        assertThat(requestCaptor.getValue().getBody().getFirst("message")).isEqualTo("OTP 1234");
    }

    @Test
    void sendSMS_nullMessage_doesNotCallGateway() {
        smsService.sendSMS(new Sms(MOBILE, null, null, null, null));

        verify(restTemplate, never()).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));
    }

    @Test
    void sendSMS_emptyMessage_doesNotCallGateway() {
        smsService.sendSMS(new Sms(MOBILE, "", null, null, null));

        verify(restTemplate, never()).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));
    }

    @Test
    void sendSMS_gatewayRejects_throwsRuntimeException() {
        Sms sms = new Sms(MOBILE, "OTP 1234", null, null, null);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<String>("code=ERR001 & info=rejected", HttpStatus.OK));

        assertThatThrownBy(() -> smsService.sendSMS(sms))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("rejected");
    }
}
