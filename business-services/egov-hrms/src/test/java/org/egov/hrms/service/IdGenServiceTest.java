package org.egov.hrms.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.hrms.config.PropertiesManager;
import org.egov.hrms.model.Employee;
import org.egov.hrms.repository.RestCallRepository;
import org.egov.hrms.web.contract.EmployeeRequest;
import org.egov.hrms.web.contract.IdGenerationResponse;
import org.egov.hrms.web.contract.IdResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdGenServiceTest {

    @Mock
    private RestCallRepository repository;

    @Mock
    private PropertiesManager properties;

    @InjectMocks
    private IdGenService idGenService;

    @Test
    void setIds_whenAllEmployeesHaveCode_skipsIdGen() {
        Employee employee = Employee.builder().code("EMP-001").tenantId("pb.amritsar").build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(new RequestInfo())
                .employees(Collections.singletonList(employee))
                .build();

        idGenService.setIds(request);

        assertThat(employee.getCode()).isEqualTo("EMP-001");
        verify(repository, never()).fetchResult(any(), any());
    }

    @Test
    void setIds_whenCodeMissing_assignsGeneratedId() {
        when(properties.getHrmsIdGenKey()).thenReturn("hrms.id");
        when(properties.getHrmsIdGenFormat()).thenReturn("EMP-[seq]");
        when(properties.getIdGenHost()).thenReturn("http://idgen/");
        when(properties.getIdGenEndpoint()).thenReturn("_generate");

        Map<String, Object> raw = new HashMap<>();
        raw.put("idResponses", List.of(Map.of("id", "EMP-100")));
        when(repository.fetchResult(any(), any())).thenReturn(raw);

        Employee employee = Employee.builder().tenantId("pb.amritsar").build();
        EmployeeRequest request = EmployeeRequest.builder()
                .requestInfo(new RequestInfo())
                .employees(Collections.singletonList(employee))
                .build();

        idGenService.setIds(request);

        assertThat(employee.getCode()).isEqualTo("EMP-100");
    }

    @Test
    void getId_whenRepositoryFails_throwsCustomException() {
        when(properties.getIdGenHost()).thenReturn("http://idgen/");
        when(properties.getIdGenEndpoint()).thenReturn("_generate");
        when(repository.fetchResult(any(), any())).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> idGenService.getId(new RequestInfo(), "pb", 1, "key", "fmt"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void getId_returnsParsedResponse() {
        when(properties.getIdGenHost()).thenReturn("http://idgen/");
        when(properties.getIdGenEndpoint()).thenReturn("_generate");
        Map<String, Object> raw = new HashMap<>();
        raw.put("idResponses", List.of(Map.of("id", "EMP-200")));
        when(repository.fetchResult(any(), any())).thenReturn(raw);

        IdGenerationResponse response = idGenService.getId(new RequestInfo(), "pb", 1, "key", "fmt");

        assertThat(response.getIdResponses()).hasSize(1);
        assertThat(response.getIdResponses().get(0).getId()).isEqualTo("EMP-200");
    }
}
