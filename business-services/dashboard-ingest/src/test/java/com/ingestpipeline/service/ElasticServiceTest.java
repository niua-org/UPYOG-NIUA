package com.ingestpipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ingestpipeline.model.IncomingData;
import com.ingestpipeline.model.TargetData;
import com.ingestpipeline.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticServiceTest {

    @Mock
    private RetryTemplate retryTemplate;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private IngestService ingestService;

    @InjectMocks
    private ElasticService elasticService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(elasticService, "indexServiceHost", "http://localhost:9200/");
        ReflectionTestUtils.setField(elasticService, "indexServiceHostSearch", "/_search");
        ReflectionTestUtils.setField(elasticService, "indexerServiceHost", "http://localhost:9200/");
        ReflectionTestUtils.setField(elasticService, "collectionIndexName", "collectionsindex-v1");
        ReflectionTestUtils.setField(elasticService, "targetIndexName", "target-index");
        ReflectionTestUtils.setField(elasticService, "DOC_PATH", "_doc");
        ReflectionTestUtils.setField(elasticService, "userName", "elastic");
        ReflectionTestUtils.setField(elasticService, "password", "secret");
        ReflectionTestUtils.setField(elasticService, "searchQueryCollection", "{\"query\":{\"match_all\":{}}}");
        ReflectionTestUtils.setField(elasticService, "searchQueryBilling", "{\"query\":{\"match_all\":{}}}");
        ReflectionTestUtils.setField(elasticService, "searchQueryPayment", "{\"query\":{\"match_all\":{}}}");
    }

    @Test
    void getESEncodedCredentials_returnsBasicAuthHeaderValue() {
        assertThat(elasticService.getESEncodedCredentials()).startsWith("Basic ");
    }

    @Test
    void searchQueryAccessors_roundTrip() {
        elasticService.setSearchQueryCollection("collection-q");
        elasticService.setSearchQueryBilling("billing-q");
        elasticService.setSearchQueryPayment("payment-q");

        assertThat(elasticService.getSearchQueryCollection()).isEqualTo("collection-q");
        assertThat(elasticService.getSearchQueryBilling()).isEqualTo("billing-q");
        assertThat(elasticService.getSearchQueryPayment()).isEqualTo("payment-q");
    }

    @Test
    void post_withAuthToken_usesRetryTemplate() {
        when(retryTemplate.postForEntity(anyString(), any(HttpEntity.class)))
                .thenReturn(new ResponseEntity<>(Map.of("result", "created"), HttpStatus.OK));

        ResponseEntity<Object> response = elasticService.post("idx", "type", "1", "token-abc", "{\"a\":1}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(retryTemplate).postForEntity(eq("http://localhost:9200/idx/type/1"), any(HttpEntity.class));
    }

    @Test
    void post_httpClientError_returnsErrorStatus() {
        when(retryTemplate.postForEntity(anyString(), any(HttpEntity.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad", null, null, StandardCharsets.UTF_8));

        ResponseEntity<Object> response = elasticService.post("idx", "type", "1", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void search_restTemplateQuery_returnsFirstHit() throws Exception {
        Map<String, Object> hit = Map.of("_id", "1", "_source", Map.of("Data", Map.of("k", "v")));
        Map<String, Object> body = Map.of(
                "hits", Map.of(
                        "total", Map.of("value", 1),
                        "hits", List.of(hit)
                )
        );
        when(retryTemplate.postForEntity(anyString(), any(HttpEntity.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        Map<?, ?> result = elasticService.search("collectionsindex-v1", "{\"query\":{}}");

        assertThat(result).isEqualTo(hit);
    }

    @Test
    void searchMultiple_returnsHitList() throws Exception {
        List<Object> hits = List.of(Map.of("_source", Map.of("Data", Map.of("x", 1))));
        Map<String, Object> body = Map.of(
                "hits", Map.of(
                        "total", Map.of("value", 2),
                        "hits", hits
                )
        );
        when(retryTemplate.postForEntity(anyString(), any(HttpEntity.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        List<?> result = elasticService.searchMultiple("collectionsindex-v1", "{\"query\":{}}");

        assertThat(result).isEqualTo(hits);
    }

    @Test
    void push_collectionDocumentCreated_returnsTrue() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put(Constants.IDENTIFIER, "doc-1");
        payload.put(Constants.DATA_OBJECT, Map.of(Constants.TRANSACTION_ID, "tx-9"));
        when(retryTemplate.postForEntity(anyString(), any(HttpEntity.class)))
                .thenReturn(new ResponseEntity<>(Map.of("created", true), HttpStatus.CREATED));

        assertThat(elasticService.push(payload)).isTrue();
    }

    @Test
    void push_targetDataCreated_returnsTrue() throws Exception {
        TargetData targetData = new TargetData();
        targetData.setId(42);
        when(retryTemplate.postForEntity(anyString(), any(HttpEntity.class)))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.CREATED));

        assertThat(elasticService.push(targetData)).isTrue();
    }

    @Test
    void push_clientError_returnsFalse() throws Exception {
        Map<String, Object> payload = Map.of(
                Constants.IDENTIFIER, "doc-1",
                Constants.DATA_OBJECT, Map.of(Constants.TRANSACTION_ID, "tx-9")
        );
        when(retryTemplate.postForEntity(anyString(), any(HttpEntity.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "conflict", null, null, StandardCharsets.UTF_8));

        assertThat(elasticService.push(payload)).isFalse();
    }

    @Test
    void search_noHits_returnsNull() throws Exception {
        Map<String, Object> body = Map.of(
                "hits", Map.of(
                        "total", Map.of("value", 0),
                        "hits", List.of()
                )
        );
        when(retryTemplate.postForEntity(anyString(), any(HttpEntity.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertThat(elasticService.search("collectionsindex-v1", "{}")).isNull();
    }

    @Test
    void searchIndex_collectionIndex_startsScrollPipeline() throws Exception {
        Map<String, Object> sourceDoc = Map.of("tenantId", "pb.test");
        Map<String, Object> innerHit = Map.of("_source", sourceDoc);
        Map<String, Object> scrollBody = Map.of(
                "_scroll_id", "scroll-1",
                "hits", Map.of("total", 1, "hits", List.of(innerHit))
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(scrollBody, HttpStatus.OK));

        assertThat(elasticService.searchIndex(Constants.ES_INDEX_COLLECTION, "{}", "v1")).isTrue();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ingestService).ingestToPipeline(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(IncomingData.class);
        IncomingData incoming = (IncomingData) captor.getValue();
        assertThat(incoming.getDataContext()).isEqualTo(Constants.DataContexts.COLLECTION);
        assertThat(incoming.getDataContextVersion()).isEqualTo("v1");
    }

    @Test
    void search_objectNodeQuery_buildsRequestViaDefaultMethod() throws Exception {
        ObjectNode query = new ObjectMapper().readValue("""
                {"query":{"bool":{"must":[{"match_phrase":{"tenantId":"pb"}}]}}}
                """, ObjectNode.class);

        // High-level client path is not exercised without a live ES node; ensure query parsing succeeds.
        assertThat(elasticService.buildSearchRequest("collectionsindex-v1", query).indices()[0])
                .isEqualTo("collectionsindex-v1");
    }
}
