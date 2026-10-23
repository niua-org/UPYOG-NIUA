package org.egov.hrms.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.hrms.config.PropertiesManager;
import org.egov.hrms.model.Employee;
import org.egov.hrms.model.Jurisdiction;
import org.egov.hrms.producer.HRMSProducer;
import org.egov.hrms.repository.EmployeeRepository;
import org.egov.hrms.utils.HRMSUtils;
import org.egov.hrms.utils.ResponseInfoFactory;
import org.egov.hrms.web.contract.EmployeeRequest;
import org.egov.hrms.web.contract.EmployeeResponse;
import org.egov.hrms.web.contract.EmployeeSearchCriteria;
import org.egov.hrms.web.contract.UserRequest;
import org.egov.hrms.web.contract.UserResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private IdGenService idGenService;
    @Mock
    private ResponseInfoFactory factory;
    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private HRMSProducer hrmsProducer;
    @Mock
    private EmployeeRepository repository;
    @Mock
    private HRMSUtils hrmsUtils;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EmployeeService employeeService;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("creator-uuid").userName("admin").build())
                .build();
        when(factory.createResponseInfoFromRequestInfo(any(), any(Boolean.class)))
                .thenReturn(ResponseInfo.builder().status("successful").build());
    }

    @Test
    void getPosition_delegatesToRepository() {
        when(repository.fetchPosition()).thenReturn(42L);
        assertThat(employeeService.getPosition()).isEqualTo(42L);
    }

    @Test
    void search_withTenantOnly_fetchesEmployees() {
        Employee employee = Employee.builder().uuid("emp-uuid").build();
        when(repository.fetchEmployees(any(), any())).thenReturn(Collections.singletonList(employee));
        when(userService.getUser(any(), any())).thenReturn(UserResponse.builder()
                .user(Collections.singletonList(org.egov.hrms.web.contract.User.builder().uuid("emp-uuid").build()))
                .build());

        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .build();

        EmployeeResponse response = employeeService.search(criteria, requestInfo);

        assertThat(response.getEmployees()).hasSize(1);
        assertThat(response.getEmployees().get(0).getUser()).isNotNull();
    }

    @Test
    void getEmployeeCountResponse_whenNoRecords_throws() {
        when(repository.fetchEmployeeCount("pb.amritsar")).thenReturn(Map.of("totalEmployee", "0"));

        assertThatThrownBy(() -> employeeService.getEmployeeCountResponse(requestInfo, "pb.amritsar"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void getEmployeeCountResponse_returnsCount() {
        when(repository.fetchEmployeeCount("pb.amritsar")).thenReturn(Map.of("totalEmployee", "5"));

        Map<String, Object> response = employeeService.getEmployeeCountResponse(requestInfo, "pb.amritsar");

        assertThat(response).containsKey("EmployeCount");
    }

    @Test
    void create_enrichesAndPushesEmployee() {
        doNothing().when(idGenService).setIds(any());
        when(propertiesManager.isAutoGeneratePassword()).thenReturn(false);
        when(propertiesManager.getSaveEmployeeTopic()).thenReturn("save-topic");
        org.egov.hrms.web.contract.User user = org.egov.hrms.web.contract.User.builder()
                .name("Alice")
                .mobileNumber("9999999999")
                .password("pwd")
                .build();
        Employee employee = Employee.builder()
                .code("EMP-001")
                .tenantId("pb.amritsar")
                .user(user)
                .jurisdictions(Collections.singletonList(Jurisdiction.builder().build()))
                .build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(requestInfo)
                .employees(Collections.singletonList(employee))
                .build();

        when(userService.createUser(any(UserRequest.class))).thenReturn(UserResponse.builder()
                .user(Collections.singletonList(org.egov.hrms.web.contract.User.builder()
                        .id(1L)
                        .uuid("new-uuid")
                        .userServiceUuid("usvc-uuid")
                        .build()))
                .build());

        EmployeeResponse response = employeeService.create(request);

        assertThat(response.getEmployees()).hasSize(1);
        assertThat(response.getEmployees().get(0).getUuid()).isEqualTo("new-uuid");
        verify(hrmsProducer).push(any(), any());
        verify(notificationService).sendNotification(any(), any());
    }

    @Test
    void update_pushesUpdateTopic() {
        when(propertiesManager.getUpdateTopic()).thenReturn("update-topic");

        org.egov.hrms.web.contract.User user = org.egov.hrms.web.contract.User.builder()
                .name("Alice")
                .active(true)
                .build();
        Employee employee = Employee.builder()
                .uuid("emp-uuid")
                .code("EMP-001")
                .IsActive(true)
                .user(user)
                .jurisdictions(Collections.emptyList())
                .assignments(Collections.emptyList())
                .build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(requestInfo)
                .employees(Collections.singletonList(employee))
                .build();

        when(repository.fetchEmployees(any(), any())).thenReturn(Collections.singletonList(
                Employee.builder()
                        .uuid("emp-uuid")
                        .jurisdictions(Collections.emptyList())
                        .assignments(Collections.emptyList())
                        .build()
        ));
        when(userService.updateUser(any(UserRequest.class))).thenReturn(new UserResponse());
        when(userService.getUser(any(), any())).thenReturn(UserResponse.builder()
                .user(Collections.singletonList(org.egov.hrms.web.contract.User.builder().uuid("emp-uuid").build()))
                .build());

        EmployeeResponse response = employeeService.update(request);

        assertThat(response.getEmployees()).hasSize(1);
        verify(hrmsProducer).push(any(), any());
    }
}
