package org.upyog.dashboard.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.upyog.dashboard.config.DashboardExtractorProperties;

@ExtendWith(MockitoExtension.class)
class DatabaseQueryExecutorTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private DashboardExtractorProperties dashboardProperties;

    @InjectMocks
    private DatabaseQueryExecutor executor;

    @Test
    @DisplayName("executeQueryWithRetry returns query results on first attempt")
    void executeQueryWithRetry_successOnFirstAttempt() {
        when(dashboardProperties.getDbMaxAttempts()).thenReturn(3);
        when(dashboardProperties.getDbBaseDelayMs()).thenReturn(1L);
        when(dashboardProperties.getDbMaxDelayMs()).thenReturn(5L);
        when(namedParameterJdbcTemplate.query(eq("SELECT 1"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of("ok"));

        List<String> result = executor.executeQueryWithRetry(
                "SELECT 1",
                new MapSqlParameterSource(),
                (rs, rowNum) -> "ok",
                "TestContext");

        assertThat(result).containsExactly("ok");
        verify(namedParameterJdbcTemplate, times(1)).query(eq("SELECT 1"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    @DisplayName("executeQueryWithRetry retries after transient failure then succeeds")
    void executeQueryWithRetry_retriesThenSucceeds() {
        when(dashboardProperties.getDbMaxAttempts()).thenReturn(3);
        when(dashboardProperties.getDbBaseDelayMs()).thenReturn(1L);
        when(dashboardProperties.getDbMaxDelayMs()).thenReturn(5L);
        when(namedParameterJdbcTemplate.query(eq("SELECT 1"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new RuntimeException("connection reset"))
                .thenReturn(List.of("ok"));

        List<String> result = executor.executeQueryWithRetry(
                "SELECT 1",
                new MapSqlParameterSource(),
                (rs, rowNum) -> "ok",
                "TestContext");

        assertThat(result).containsExactly("ok");
        verify(namedParameterJdbcTemplate, times(2)).query(eq("SELECT 1"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    @DisplayName("executeQueryWithRetry throws after max attempts exhausted")
    void executeQueryWithRetry_throwsAfterMaxAttempts() {
        when(dashboardProperties.getDbMaxAttempts()).thenReturn(2);
        when(dashboardProperties.getDbBaseDelayMs()).thenReturn(1L);
        when(dashboardProperties.getDbMaxDelayMs()).thenReturn(5L);
        when(namedParameterJdbcTemplate.query(eq("SELECT 1"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> executor.executeQueryWithRetry(
                "SELECT 1",
                new MapSqlParameterSource(),
                (rs, rowNum) -> "ok",
                "TestContext"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");
    }
}
