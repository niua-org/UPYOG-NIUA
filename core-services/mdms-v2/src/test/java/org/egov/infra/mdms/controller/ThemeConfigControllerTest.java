package org.egov.infra.mdms.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.model.ThemeConfig;
import org.egov.infra.mdms.model.ThemeConfigWorkflowRequest;
import org.egov.infra.mdms.service.ThemeConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThemeConfigControllerTest {

    @Mock
    private ThemeConfigService themeConfigService;

    @InjectMocks
    private ThemeConfigController controller;

    private static ThemeConfigWorkflowRequest workflowRequest(ThemeConfig theme, RequestInfo requestInfo) {
        ThemeConfigWorkflowRequest request = new ThemeConfigWorkflowRequest();
        request.setThemeConfig(theme);
        request.setRequestInfo(requestInfo);
        return request;
    }

    @Test
    void create_success_returnsOkWithId() {
        ThemeConfig theme = ThemeConfig.builder().tenantId("pg").themeType("LOGIN").build();
        ThemeConfigWorkflowRequest request = workflowRequest(theme, RequestInfo.builder().build());
        ThemeConfig created = ThemeConfig.builder().id("theme-1").build();
        when(themeConfigService.create(theme, request.getRequestInfo())).thenReturn(created);

        ResponseEntity<?> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("id")).isEqualTo("theme-1");
    }

    @Test
    void create_failure_returnsInternalServerError() {
        ThemeConfig theme = ThemeConfig.builder().tenantId("pg").themeType("LOGIN").build();
        ThemeConfigWorkflowRequest request = workflowRequest(theme, RequestInfo.builder().build());
        when(themeConfigService.create(theme, request.getRequestInfo()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void search_returnsThemeList() {
        ThemeConfig theme = ThemeConfig.builder().tenantId("pg").themeType("LOGIN").isActive(true).build();
        ThemeConfigWorkflowRequest request = workflowRequest(theme, null);
        when(themeConfigService.search("pg", "LOGIN", true))
                .thenReturn(List.of(ThemeConfig.builder().id("t1").build()));

        ResponseEntity<?> response = controller.search(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) response.getBody()).hasSize(1);
    }
}
