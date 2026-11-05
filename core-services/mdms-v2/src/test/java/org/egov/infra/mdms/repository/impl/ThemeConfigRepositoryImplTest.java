package org.egov.infra.mdms.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.infra.mdms.config.ApplicationConfig;
import org.egov.infra.mdms.model.ThemeConfig;
import org.egov.infra.mdms.producer.Producer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThemeConfigRepositoryImplTest {

    @Mock
    private Producer producer;
    @Mock
    private ApplicationConfig applicationConfig;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private ThemeConfigRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ThemeConfigRepositoryImpl(producer, applicationConfig, jdbcTemplate, new ObjectMapper());
    }

    @Test
    void create_pushesSaveTopic() {
        when(applicationConfig.getSaveThemeConfigTopicName()).thenReturn("save-theme");
        ThemeConfig theme = ThemeConfig.builder().id("t1").build();

        repository.create(theme);

        verify(producer).push("save-theme", theme);
    }

    @Test
    void update_pushesUpdateTopic() {
        when(applicationConfig.getUpdateThemeConfigStatusTopicName()).thenReturn("update-theme");
        ThemeConfig theme = ThemeConfig.builder().id("t1").build();

        repository.update(theme);

        verify(producer).push("update-theme", theme);
    }

    @Test
    void createStaging_pushesStagingTopic() {
        when(applicationConfig.getSaveThemeConfigStagingTopicName()).thenReturn("staging-theme");
        ThemeConfig theme = ThemeConfig.builder().id("t1").build();

        repository.createStaging(theme);

        verify(producer).push("staging-theme", theme);
    }

    @Test
    void existsPendingTheme_countGreaterThanZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(2);

        assertThat(repository.existsPendingTheme("pg", "LOGIN")).isTrue();
    }

    @Test
    void existsTheme_countZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(0);

        assertThat(repository.existsTheme("pg", "LOGIN")).isFalse();
    }

    @Test
    void deactivateAllThemes_runsUpdate() {
        repository.deactivateAllThemes("pg", "LOGIN");

        verify(jdbcTemplate).update(anyString(), eq("pg"), eq("LOGIN"));
    }

    @Test
    void setDefaultTheme_deactivatesThenActivatesSelected() {
        repository.setDefaultTheme("id-1", "pg", "LOGIN");

        verify(jdbcTemplate).update(anyString(), eq("pg"), eq("LOGIN"));
        verify(jdbcTemplate).update(anyString(), eq("id-1"), eq("pg"), eq("LOGIN"));
    }

    @Test
    void search_activeFilter_appendsClause() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(Collections.emptyList());

        List<ThemeConfig> result = repository.search("pg", "LOGIN", true);

        assertThat(result).isEmpty();
    }
}
