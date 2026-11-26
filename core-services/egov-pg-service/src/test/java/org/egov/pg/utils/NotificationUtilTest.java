package org.egov.pg.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.pg.config.AppProperties;
import org.egov.pg.producer.Producer;
import org.egov.pg.repository.ServiceCallRepository;
import org.egov.pg.web.models.SMSRequest;
import org.egov.pg.models.Transaction;
import org.egov.pg.web.models.TransactionRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NotificationUtilTest {

    @Mock
    private ServiceCallRepository serviceCallRepository;
    @Mock
    private AppProperties appProperties;
    @Mock
    private Producer producer;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationUtil notificationUtil;

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        try {
            var field = NotificationUtil.class.getDeclaredField("mapper");
            field.setAccessible(true);
            field.set(notificationUtil, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(appProperties.getLocalizationHost()).thenReturn("http://local");
        when(appProperties.getLocalizationContextPath()).thenReturn("/loc");
        when(appProperties.getLocalizationSearchEndpoint()).thenReturn("/search");
        when(appProperties.getIsLocalizationStateLevel()).thenReturn(false);
        when(appProperties.getUrlShortnerHost()).thenReturn("http://short");
        when(appProperties.getUrlShortnerEndpoint()).thenReturn("/api");
        when(appProperties.getBillingServiceHost()).thenReturn("http://bill");
        when(appProperties.getBillingServiceSearchEndpoint()).thenReturn("/search");
        when(appProperties.getSmsNotifTopic()).thenReturn("sms-topic");
        when(appProperties.getIsSMSEnable()).thenReturn(true);
    }

    @Test
    public void getUriBuildsLocalizationUrl() {
        RequestInfo requestInfo = RequestInfo.builder().msgId("a|en_IN").build();
        String uri = notificationUtil.getUri("pb.amritsar", requestInfo, "egov-pg");
        assertEquals("http://local/loc/search?locale=en_IN&tenantId=pb.amritsar&module=egov-pg", uri);
    }

    @Test
    public void getUriUsesStateLevelTenantWhenConfigured() {
        when(appProperties.getIsLocalizationStateLevel()).thenReturn(true);
        String uri = notificationUtil.getUri("pb.amritsar", new RequestInfo(), "egov-pg");
        assertEquals("http://local/loc/search?locale=en_IN&tenantId=pb&module=egov-pg", uri);
    }

    @Test
    public void getCustomizedMsgParsesLocalizationPayload() {
        String localization = "{\"messages\":[{\"code\":\"PG_NOTIFICATION_SUCCESS\",\"message\":\"Paid\"}]}";
        assertEquals("Paid", notificationUtil.getCustomizedMsg("SUCCESS", localization));
        assertNull(notificationUtil.getCustomizedMsg("MISSING", localization));
    }

    @Test
    public void sendSMSPushesEachRequest() {
        notificationUtil.sendSMS(Collections.singletonList(
                SMSRequest.builder().mobileNumber("9999999999").message("hello").build()));
        verify(producer).push(eq("sms-topic"), any(SMSRequest.class));
    }

    @Test
    public void getShortnerURLReturnsOriginalWhenEmpty() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("");
        assertEquals("http://actual", notificationUtil.getShortnerURL("http://actual"));
    }

    @Test
    public void getShortnerURLReturnsShortLink() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("http://s");
        assertEquals("http://s", notificationUtil.getShortnerURL("http://actual"));
    }

    @Test
    public void getLocalizationMessagesFetchesFromService() {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("messages", Collections.emptyList());
        when(serviceCallRepository.fetchResult(anyString(), any())).thenReturn(Optional.of(body));
        String result = notificationUtil.getLocalizationMessages("pb", new RequestInfo(), "egov-pg");
        assertEquals("{\"messages\":[]}", result);
    }

    @Test
    public void getBusinessServiceReadsFromBillingResponse() {
        TransactionRequest request = new TransactionRequest(new RequestInfo(),
                Transaction.builder().tenantId("pb").billId("bill-1").build());
        LinkedHashMap<String, Object> billWrapper = new LinkedHashMap<>();
        billWrapper.put("Bill", Collections.singletonList(Collections.singletonMap("businessService", "PT")));
        when(serviceCallRepository.fetchResult(anyString(), any())).thenReturn(Optional.of(billWrapper));
        assertEquals("PT", notificationUtil.getBusinessService(request));
    }
}
