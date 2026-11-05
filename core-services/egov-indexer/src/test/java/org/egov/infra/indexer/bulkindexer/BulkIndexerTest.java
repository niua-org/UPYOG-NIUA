package org.egov.infra.indexer.bulkindexer;

import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.web.contract.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkIndexerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private IndexerUtils indexerUtils;

    @InjectMocks
    private BulkIndexer bulkIndexer;

    @BeforeEach
    void credentials() {
        lenient().when(indexerUtils.getESEncodedCredentials()).thenReturn("Basic abc");
    }

    @Test
    void indexJsonOntoES_postsDocument() throws Exception {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("result", "created"));

        bulkIndexer.indexJsonOntoES("http://es/index/_doc", "{\"a\":1}", new Index());

        verify(restTemplate).postForObject(eq("http://es/index/_doc"), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void indexJsonOntoES_bulkWithErrors_logsFailure() throws Exception {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("errors", true));

        bulkIndexer.indexJsonOntoES("http://es/index/_bulk", "{\"index\":{}}", new Index());

        verify(restTemplate).postForObject(eq("http://es/index/_bulk"), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void indexJsonOntoES_bulkWithoutErrors_succeeds() throws Exception {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("errors", false));

        bulkIndexer.indexJsonOntoES("http://es/index/_bulk", "{}", new Index());

        verify(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void indexJsonOntoES_resourceAccessException_orchestratesHealthCheck() throws Exception {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("ES down"));

        bulkIndexer.indexJsonOntoES("http://es/_bulk", "{}", new Index());

        verify(indexerUtils).orchestrateListenerOnESHealth();
    }

    @Test
    void indexJsonOntoES_genericException_swallowed() throws Exception {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("mapping error"));

        bulkIndexer.indexJsonOntoES("http://es/_bulk", "{}", new Index());
    }

    @Test
    void getIndexMappingfromES_returnsResponse() {
        Map<String, Object> mapping = Map.of("properties", Map.of());
        when(restTemplate.getForObject("http://es/index/_mapping", Map.class)).thenReturn(mapping);

        assertThat(bulkIndexer.getIndexMappingfromES("http://es/index/_mapping")).isEqualTo(mapping);
    }

    @Test
    void getIndexMappingfromES_resourceAccessException_orchestratesHealthCheck() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("ES down"));

        assertThat(bulkIndexer.getIndexMappingfromES("http://es/index/_mapping")).isNull();
        verify(indexerUtils).orchestrateListenerOnESHealth();
    }

    @Test
    void getIndexMappingfromES_otherException_returnsNull() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("bad response"));

        assertThat(bulkIndexer.getIndexMappingfromES("http://es/index/_mapping")).isNull();
    }

    @Test
    void getESResponse_post_returnsBody() {
        when(restTemplate.postForObject(eq("http://es/_search"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("hits", Map.of()));

        Object response = bulkIndexer.getESResponse("http://es/_search", Map.of("size", 10), "POST");

        assertThat(response).isInstanceOf(Map.class);
    }

    @Test
    void getESResponse_postException_returnsNull() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("post failed"));

        assertThat(bulkIndexer.getESResponse("http://es/_search", Map.of("q", 1), "POST")).isNull();
    }

    @Test
    void getESResponse_put_returnsOk() {
        Object response = bulkIndexer.getESResponse("http://es/index/_settings", Map.of("index", Map.of()), "PUT");

        assertThat(response).isEqualTo("OK");
        verify(restTemplate).put(eq("http://es/index/_settings"), any(HttpEntity.class));
    }

    @Test
    void getESResponse_putException_returnsNull() {
        doThrow(new RuntimeException("put failed")).when(restTemplate).put(anyString(), any(HttpEntity.class));

        assertThat(bulkIndexer.getESResponse("http://es/index/_settings", Map.of(), "PUT")).isNull();
    }

    @Test
    void getESResponse_getWithoutBody_exchanges() {
        when(restTemplate.exchange(eq("http://es/_search"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("ok", true)));

        Object response = bulkIndexer.getESResponse("http://es/_search", null, "GET");

        assertThat(response).isNotNull();
    }

    @Test
    void getESResponse_getException_returnsNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("get failed"));

        assertThat(bulkIndexer.getESResponse("http://es/_search", null, "GET")).isNull();
    }
}
