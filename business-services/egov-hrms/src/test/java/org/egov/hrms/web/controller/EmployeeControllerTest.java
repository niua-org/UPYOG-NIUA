package org.egov.hrms.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.hrms.service.EmployeeService;
import org.egov.hrms.web.contract.EmployeeResponse;
import org.egov.hrms.web.contract.RequestInfoWrapper;
import org.egov.hrms.web.validator.EmployeeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EmployeeService employeeService;

    @Mock
    private EmployeeValidator validator;

    @InjectMocks
    private EmployeeController employeeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(employeeController).build();
    }

    @Test
    void count_withTenantId_returnsOk() throws Exception {
        doNothing().when(validator).validateEmployeeCountRequest("pb.amritsar");
        Map<String, Object> countResponse = new HashMap<>();
        countResponse.put("count", 3);
        when(employeeService.getEmployeeCountResponse(any(RequestInfo.class), eq("pb.amritsar")))
                .thenReturn(countResponse);

        RequestInfo requestInfo = RequestInfo.builder().apiId("hrms").build();

        mockMvc.perform(post("/employees/_count")
                        .param("tenantId", "pb.amritsar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInfo)))
                .andExpect(status().isOk());
    }

    @Test
    void search_withCriteria_returnsOk() throws Exception {
        doNothing().when(validator).validateSearchRequest(any(), any());
        when(employeeService.search(any(), any())).thenReturn(new EmployeeResponse());

        RequestInfoWrapper wrapper = RequestInfoWrapper.builder()
                .requestInfo(RequestInfo.builder().apiId("hrms").build())
                .build();

        mockMvc.perform(post("/employees/_search")
                        .param("tenantId", "pb.amritsar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isOk());
    }
}
