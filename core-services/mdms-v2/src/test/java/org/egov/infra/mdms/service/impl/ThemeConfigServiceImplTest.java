package org.egov.infra.mdms.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.model.ThemeConfig;
import org.egov.infra.mdms.repository.ThemeConfigRepository;
import org.egov.infra.mdms.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThemeConfigServiceImplTest {

    @Mock
    private ThemeConfigRepository themeConfigRepository;
    @Mock
    private WorkflowService workflowService;

    private ThemeConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ThemeConfigServiceImpl(themeConfigRepository, workflowService);
    }

    @Test
    void create_firstThemeForTenant_setsSystemDefaultAndPersists() {
        ThemeConfig input = ThemeConfig.builder().tenantId("pg.citya").themeType("LOGIN").build();
        when(themeConfigRepository.existsTheme("pg.citya", "LOGIN")).thenReturn(false);
        when(themeConfigRepository.existsActiveTheme("pg.citya", "LOGIN")).thenReturn(false);

        ThemeConfig created = service.create(input, RequestInfo.builder().build());

        assertThat(created.getThemeName()).isEqualTo("System Default");
        assertThat(created.getStatus()).isEqualTo("DEFAULT");
        assertThat(created.getIsActive()).isTrue();
        assertThat(created.getId()).isNotBlank();
        verify(themeConfigRepository).deactivateAllThemes("pg.citya", "LOGIN");
        verify(themeConfigRepository).create(created);
    }

    @Test
    void create_duplicateThemeName_throwsRuntimeException() {
        ThemeConfig input = ThemeConfig.builder()
                .tenantId("pg.citya")
                .themeType("LOGIN")
                .themeName("City Theme")
                .build();
        when(themeConfigRepository.existsTheme("pg.citya", "LOGIN")).thenReturn(true);
        when(themeConfigRepository.existsActiveTheme("pg.citya", "LOGIN")).thenReturn(true);
        when(themeConfigRepository.existsThemeName("pg.citya", "LOGIN", "City Theme")).thenReturn(true);

        assertThatThrownBy(() -> service.create(input, RequestInfo.builder().build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void update_missingThemeName_throwsRuntimeException() {
        ThemeConfig input = ThemeConfig.builder().tenantId("pg.citya").themeType("LOGIN").build();

        assertThatThrownBy(() -> service.update(input, RequestInfo.builder().build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Theme name is mandatory");
    }

    @Test
    void update_validRequest_createsPendingStagingRow() {
        ThemeConfig input = ThemeConfig.builder()
                .tenantId("pg.citya")
                .themeType("LOGIN")
                .themeName("City Theme")
                .build();
        when(themeConfigRepository.existsThemeName("pg.citya", "LOGIN", "City Theme")).thenReturn(false);
        when(themeConfigRepository.existsPendingTheme("pg.citya", "LOGIN")).thenReturn(false);
        when(workflowService.createWorkflow(any(), any())).thenReturn("wf-1");

        ThemeConfig updated = service.update(input, RequestInfo.builder().build());

        assertThat(updated.getStatus()).isEqualTo("PENDING");
        assertThat(updated.getWorkflowId()).isEqualTo("wf-1");
        verify(themeConfigRepository).createStaging(updated);
    }

    @Test
    void updateWorkflowStatus_setDefault_activatesTheme() {
        ThemeConfig input = ThemeConfig.builder()
                .id("theme-1")
                .tenantId("pg.citya")
                .themeType("LOGIN")
                .build();

        ThemeConfig result = service.updateWorkflowStatus(
                input, "SET_DEFAULT", RequestInfo.builder().build());

        assertThat(result.getIsActive()).isTrue();
        verify(themeConfigRepository).setDefaultTheme("theme-1", "pg.citya", "LOGIN");
    }

    @Test
    void search_delegatesToRepository() {
        when(themeConfigRepository.search("pg.citya", "LOGIN", true))
                .thenReturn(java.util.List.of(ThemeConfig.builder().id("t1").build()));

        assertThat(service.search("pg.citya", "LOGIN", true)).hasSize(1);
    }
}
