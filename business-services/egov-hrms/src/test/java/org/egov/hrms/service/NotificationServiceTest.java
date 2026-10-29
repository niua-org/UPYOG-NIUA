package org.egov.hrms.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.hrms.model.Employee;
import org.egov.hrms.model.ReactivationDetails;
import org.egov.hrms.producer.HRMSProducer;
import org.egov.hrms.repository.RestCallRepository;
import org.egov.hrms.utils.HRMSConstants;
import org.egov.hrms.web.contract.EmployeeRequest;
import org.egov.hrms.web.contract.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private HRMSProducer producer;
    @Mock
    private RestCallRepository repository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "smsTopic", "sms-topic");
        ReflectionTestUtils.setField(notificationService, "appLink", "http://app/");
        ReflectionTestUtils.setField(notificationService, "localizationHost", "http://loc/");
        ReflectionTestUtils.setField(notificationService, "localizationSearchEndpoint", "v1/_search");
        ReflectionTestUtils.setField(notificationService, "otpHost", "http://otp/");
        ReflectionTestUtils.setField(notificationService, "otpCreateEndpoint", "v1/_create");
        ReflectionTestUtils.setField(notificationService, "envHost", "http://env/");
    }

    @Test
    void buildMessage_replacesPlaceholders() {
        Employee employee = Employee.builder()
                .code("EMP1")
                .uuid("uuid-1")
                .user(User.builder().name("Alice").build())
                .build();
        Map<String, String> pwdMap = Map.of("uuid-1", "secret");

        String message = notificationService.buildMessage(employee, "Hi $employeename ($username) pwd=$password link=$applink", pwdMap);

        assertThat(message).contains("Alice").contains("EMP1").contains("secret").contains("http://app/");
    }

    @Test
    void getLocalisedMessages_parsesLocalizationResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("messages", List.of(
                Map.of("code", HRMSConstants.HRMS_EMP_CREATE_LOCLZN_CODE, "message", "Welcome $username")
        ));
        when(repository.fetchResult(any(), any())).thenReturn(result);

        Map<String, Map<String, String>> localized = notificationService.getLocalisedMessages(
                new RequestInfo(), "pb.amritsar", HRMSConstants.HRMS_LOCALIZATION_ENG_LOCALE_CODE,
                HRMSConstants.HRMS_LOCALIZATION_MODULE_CODE);

        assertThat(localized.get("en_IN|pb")).containsEntry(HRMSConstants.HRMS_EMP_CREATE_LOCLZN_CODE, "Welcome $username");
    }

    @Test
    void getMessage_returnsLocalizedTemplate() {
        Map<String, Object> result = new HashMap<>();
        result.put("messages", List.of(
                Map.of("code", HRMSConstants.HRMS_EMP_CREATE_LOCLZN_CODE, "message", "Welcome $username")
        ));
        when(repository.fetchResult(any(), any())).thenReturn(result);

        Employee employee = Employee.builder()
                .tenantId("pb.amritsar")
                .code("EMP1")
                .user(User.builder().mobileNumber("9999999999").name("Bob").build())
                .build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(new RequestInfo())
                .employees(Collections.singletonList(employee))
                .build();

        assertThat(notificationService.getMessage(request, HRMSConstants.HRMS_EMP_CREATE_LOCLZN_CODE))
                .isEqualTo("Welcome $username");
    }

    @Test
    void getOTP_returnsOtpFromResponse() {
        Employee employee = Employee.builder()
                .tenantId("pb.amritsar")
                .user(User.builder().mobileNumber("8888888888").build())
                .build();
        when(restTemplate.postForObject(any(String.class), any(), eq(Map.class)))
                .thenReturn(Map.of("otp", Map.of("otp", "123456")));

        assertThat(notificationService.getOTP(employee, new RequestInfo())).isEqualTo("123456");
    }

    @Test
    void sendNotification_whenMessageConfigured_pushesSms() {
        Map<String, Object> locResult = new HashMap<>();
        locResult.put("messages", List.of(
                Map.of("code", HRMSConstants.HRMS_EMP_CREATE_LOCLZN_CODE,
                        "message", "Hi $employeename ($username) pwd=$password link=$applink")
        ));
        when(repository.fetchResult(any(), any())).thenReturn(locResult);

        Employee employee = Employee.builder()
                .tenantId("pb.amritsar")
                .code("EMP1")
                .uuid("uuid-1")
                .user(User.builder().mobileNumber("9999999999").name("Bob").build())
                .build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(new RequestInfo())
                .employees(Collections.singletonList(employee))
                .build();

        notificationService.sendNotification(request, Map.of("uuid-1", "secret"));

        verify(producer).push(eq("sms-topic"), any());
    }

    @Test
    void sendNotification_whenMessageMissing_skipsProducer() {
        when(repository.fetchResult(any(), any())).thenReturn(Map.of("messages", List.of()));

        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(new RequestInfo())
                .employees(Collections.singletonList(Employee.builder()
                        .tenantId("pb.amritsar")
                        .user(User.builder().mobileNumber("9999999999").build())
                        .build()))
                .build();

        notificationService.sendNotification(request, Map.of());

        verify(producer, never()).push(any(), any());
    }

    @Test
    void sendReactivationNotification_sendsSmsForEligibleEmployee() {
        Map<String, Object> locResult = new HashMap<>();
        locResult.put("messages", List.of(
                Map.of("code", HRMSConstants.HRMS_EMP_REACTIVATE_LOCLZN_CODE,
                        "message", "Hi {Employee Name} {Username} {date} {password} {link}")
        ));
        when(repository.fetchResult(any(), any())).thenReturn(locResult);
        when(restTemplate.postForObject(any(String.class), any(), eq(Map.class)))
                .thenReturn(Map.of("otp", Map.of("otp", "654321")));

        ReactivationDetails reactivationDetails = ReactivationDetails.builder()
                .effectiveFrom(System.currentTimeMillis())
                .build();
        Employee employee = Employee.builder()
                .tenantId("pb.amritsar")
                .code("EMP9")
                .reActivateEmployee(true)
                .reactivationDetails(Collections.singletonList(reactivationDetails))
                .user(User.builder().mobileNumber("7777777777").name("Carol").build())
                .build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(new RequestInfo())
                .employees(Collections.singletonList(employee))
                .build();

        notificationService.sendReactivationNotification(request);

        verify(producer).push(eq("sms-topic"), any());
    }
}
