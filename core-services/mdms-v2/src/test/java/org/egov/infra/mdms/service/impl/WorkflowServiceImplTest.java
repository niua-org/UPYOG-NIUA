package org.egov.infra.mdms.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.config.ApplicationConfig;
import org.egov.infra.mdms.model.ThemeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceImplTest {

    @Mock
    private ApplicationConfig applicationConfig;

    @Mock
    private RestTemplate restTemplate;

    private WorkflowServiceImpl workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowServiceImpl(applicationConfig);
        setRestTemplate(workflowService, restTemplate);
        when(applicationConfig.getWorkflowHost()).thenReturn("http://workflow");
        when(applicationConfig.getWorkflowTransitionEndpoint()).thenReturn("/transition");
        when(applicationConfig.getThemeConfigBusinessService()).thenReturn("THEME_CONFIG");
    }

    @Test
    void createWorkflow_success_returnsWorkflowId() {
        Map<String, Object> response = Map.of(
                "ProcessInstances", List.of(Map.of("id", "wf-123")));
        when(restTemplate.postForObject(eq("http://workflow/transition"), any(), eq(Map.class)))
                .thenReturn(response);

        String workflowId = workflowService.createWorkflow(themeConfig(), RequestInfo.builder().build());

        assertThat(workflowId).isEqualTo("wf-123");
        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForObject(eq("http://workflow/transition"), requestCaptor.capture(), eq(Map.class));
        assertThat(requestCaptor.getValue()).isNotNull();
    }

    @Test
    void createWorkflow_emptyResponse_returnsNull() {
        when(restTemplate.postForObject(any(), any(), eq(Map.class))).thenReturn(new HashMap<>());

        String workflowId = workflowService.createWorkflow(themeConfig(), RequestInfo.builder().build());

        assertThat(workflowId).isNull();
    }

    @Test
    void createWorkflow_restFailure_returnsNull() {
        when(restTemplate.postForObject(any(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("down"));

        String workflowId = workflowService.createWorkflow(themeConfig(), RequestInfo.builder().build());

        assertThat(workflowId).isNull();
    }

    @Test
    void transitionWorkflow_success_returnsWorkflowId() {
        Map<String, Object> instance = new HashMap<>();
        instance.put("id", "wf-transition");
        Map<String, Object> response = new HashMap<>();
        response.put("ProcessInstances", List.of(instance));
        when(restTemplate.postForObject(eq("http://workflow/transition"), any(), eq(Map.class)))
                .thenReturn(response);

        String workflowId = workflowService.transitionWorkflow(
                themeConfig(), RequestInfo.builder().build(), "APPROVE");

        assertThat(workflowId).isEqualTo("wf-transition");
    }

    @Test
    void transitionWorkflow_missingProcessInstances_returnsNull() {
        when(restTemplate.postForObject(any(), any(), eq(Map.class))).thenReturn(Map.of("status", "ok"));

        String workflowId = workflowService.transitionWorkflow(
                themeConfig(), RequestInfo.builder().build(), "REJECT");

        assertThat(workflowId).isNull();
    }

    private static ThemeConfig themeConfig() {
        return ThemeConfig.builder()
                .id("theme-1")
                .tenantId("pg.citya")
                .build();
    }

    private static void setRestTemplate(WorkflowServiceImpl target, RestTemplate restTemplate) {
        try {
            Field field = WorkflowServiceImpl.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(target, restTemplate);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
