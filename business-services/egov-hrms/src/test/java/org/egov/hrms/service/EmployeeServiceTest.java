package org.egov.hrms.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.hrms.config.PropertiesManager;
import org.egov.hrms.model.Assignment;
import org.egov.hrms.model.AuditDetails;
import org.egov.hrms.model.DepartmentalTest;
import org.egov.hrms.model.EducationalQualification;
import org.egov.hrms.model.Employee;
import org.egov.hrms.model.EmployeeDocument;
import org.egov.hrms.model.Jurisdiction;
import org.egov.hrms.model.ServiceHistory;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void search_withEmployeeCode_resolvesThroughUserService() {
        org.egov.hrms.web.contract.User user = org.egov.hrms.web.contract.User.builder().uuid("code-uuid").build();
        when(userService.getUser(any(), anyMap())).thenReturn(UserResponse.builder()
                .user(Collections.singletonList(user))
                .build());
        when(repository.fetchEmployees(any(), any())).thenReturn(Collections.singletonList(
                Employee.builder().uuid("code-uuid").build()));

        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .codes(Collections.singletonList("EMP-CODE"))
                .build();

        EmployeeResponse response = employeeService.search(criteria, requestInfo);

        assertThat(response.getEmployees()).hasSize(1);
    }

    @Test
    void search_withPhoneFilter_intersectsUserUuids() {
        org.egov.hrms.web.contract.User user = org.egov.hrms.web.contract.User.builder().uuid("phone-uuid").build();
        when(userService.getUser(any(), anyMap())).thenReturn(UserResponse.builder()
                .user(Collections.singletonList(user))
                .build());
        Employee employee = Employee.builder().uuid("phone-uuid").build();
        when(repository.fetchEmployees(any(), any())).thenReturn(Collections.singletonList(employee));

        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .phone("9999999999")
                .build();

        EmployeeResponse response = employeeService.search(criteria, requestInfo);

        assertThat(response.getEmployees()).hasSize(1);
        assertThat(response.getEmployees().get(0).getUser().getUuid()).isEqualTo("phone-uuid");
    }

    @Test
    void search_withRoleFilterAndEmptyUsers_skipsRepositoryFetch() {
        when(userService.getUser(any(), anyMap())).thenReturn(UserResponse.builder()
                .user(Collections.emptyList())
                .build());

        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .roles(Collections.singletonList("EMPLOYEE"))
                .build();

        EmployeeResponse response = employeeService.search(criteria, requestInfo);

        assertThat(response.getEmployees()).isEmpty();
    }

    @Test
    void search_withNames_mergesUserUuidsFromMultipleCalls() {
        when(userService.getUser(any(), anyMap()))
                .thenReturn(UserResponse.builder().user(Collections.singletonList(
                        org.egov.hrms.web.contract.User.builder().uuid("n1").build())).build())
                .thenReturn(UserResponse.builder().user(Collections.singletonList(
                        org.egov.hrms.web.contract.User.builder().uuid("n2").build())).build());
        when(repository.fetchEmployees(any(), any())).thenReturn(List.of(
                Employee.builder().uuid("n1").build(),
                Employee.builder().uuid("n2").build()
        ));

        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .names(List.of("Alice", "Bob"))
                .build();

        EmployeeResponse response = employeeService.search(criteria, requestInfo);

        assertThat(response.getEmployees()).hasSize(2);
    }

    @Test
    void create_whenUserServiceFails_throwsCustomException() {
        doNothing().when(idGenService).setIds(any());
        when(propertiesManager.isAutoGeneratePassword()).thenReturn(true);
        when(hrmsUtils.generatePassword(any())).thenReturn("generated");
        org.egov.hrms.web.contract.User user = org.egov.hrms.web.contract.User.builder()
                .name("Alice")
                .mobileNumber("9999999999")
                .build();
        Employee employee = Employee.builder()
                .code("EMP-002")
                .tenantId("pb.amritsar")
                .user(user)
                .jurisdictions(Collections.singletonList(Jurisdiction.builder().build()))
                .assignments(Collections.singletonList(Assignment.builder().build()))
                .serviceHistory(Collections.singletonList(ServiceHistory.builder().build()))
                .education(Collections.singletonList(EducationalQualification.builder().build()))
                .tests(Collections.singletonList(DepartmentalTest.builder().build()))
                .documents(Collections.singletonList(EmployeeDocument.builder().build()))
                .build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(requestInfo)
                .employees(Collections.singletonList(employee))
                .build();
        when(userService.createUser(any(UserRequest.class))).thenThrow(new RuntimeException("user down"));

        assertThatThrownBy(() -> employeeService.create(request)).isInstanceOf(CustomException.class);
    }

    @Test
    void update_whenUserServiceFails_throwsCustomException() {
        when(propertiesManager.getUpdateTopic()).thenReturn("update-topic");
        org.egov.hrms.web.contract.User user = org.egov.hrms.web.contract.User.builder()
                .name("Alice")
                .active(false)
                .build();
        AuditDetails audit = AuditDetails.builder()
                .createdBy("admin")
                .createdDate(1L)
                .build();
        Jurisdiction jurisdiction = Jurisdiction.builder().id("j1").auditDetails(audit).build();
        Employee existing = Employee.builder()
                .uuid("emp-uuid")
                .jurisdictions(Collections.singletonList(jurisdiction))
                .assignments(Collections.emptyList())
                .build();
        Employee employee = Employee.builder()
                .uuid("emp-uuid")
                .code("EMP-001")
                .IsActive(false)
                .user(user)
                .jurisdictions(Collections.singletonList(jurisdiction))
                .assignments(Collections.emptyList())
                .build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(requestInfo)
                .employees(Collections.singletonList(employee))
                .build();

        when(repository.fetchEmployees(any(), any())).thenReturn(Collections.singletonList(existing));
        when(userService.getUser(any(), any())).thenReturn(UserResponse.builder()
                .user(Collections.singletonList(org.egov.hrms.web.contract.User.builder().uuid("emp-uuid").build()))
                .build());
        doThrow(new RuntimeException("update failed")).when(userService).updateUser(any(UserRequest.class));

        assertThatThrownBy(() -> employeeService.update(request)).isInstanceOf(CustomException.class);
    }
}
