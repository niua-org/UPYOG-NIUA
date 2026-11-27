package org.upyog.reconciliation.extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ElasticsearchDataExtractorTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Environment environment;

    private ElasticsearchDataExtractor extractor;

    @BeforeEach
    void setUp() throws Exception {
        extractor = new ElasticsearchDataExtractor();
        setField(extractor, "esHost", "http://localhost:9200");
        setField(extractor, "restTemplate", restTemplate);
        setField(extractor, "env", environment);
    }

    @Test
    @DisplayName("extractData returns empty list when module index is not configured")
    void extractData_missingIndex_returnsEmpty() {
        when(environment.getProperty("reconciliation.es.index.pt")).thenReturn(null);

        List<Map<String, Object>> result = extractor.extractData("pg.citya", "PT", LocalDate.of(2026, 7, 1));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("extractData maps Elasticsearch _source documents from search response")
    void extractData_searchResponse_mapsSources() {
        when(environment.getProperty("reconciliation.es.index.pt")).thenReturn("pt-index");

        Map<String, Object> source = Map.of("tenantId", "pg.citya", "assessments", 10);
        Map<String, Object> hit = Map.of("_source", source);
        Map<String, Object> hits = Map.of("hits", List.of(hit));
        Map<String, Object> body = Map.of("hits", hits);

        when(restTemplate.postForEntity(
                eq("http://localhost:9200/pt-index/_search"),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        List<Map<String, Object>> result = extractor.extractData("pg.citya", "PT", LocalDate.of(2026, 7, 1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("tenantId", "pg.citya");
    }

    @Test
    @DisplayName("extractData returns empty list when RestTemplate call fails")
    void extractData_restFailure_returnsEmpty() {
        when(environment.getProperty("reconciliation.es.index.pt")).thenReturn("pt-index");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("connection refused"));

        List<Map<String, Object>> result = extractor.extractData("pg.citya", "PT", LocalDate.of(2026, 7, 1));

        assertThat(result).isEmpty();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
