package com.ingestpipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.search.SearchRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IESServiceDefaultMethodsTest {

    private final IESService service = new ElasticService();

    @AfterEach
    void clearClients() {
        IESService.esClient.clear();
    }

    @Test
    void buildSearchRequest_translatesMatchPhraseClauses() throws IOException {
        ObjectNode query = new ObjectMapper().readValue("""
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "match_phrase": { "tenantId": "pb.amritsar" } }
                      ]
                    }
                  }
                }
                """, ObjectNode.class);

        SearchRequest request = service.buildSearchRequest("collectionsindex-v1", query);

        assertThat(request.indices()[0]).isEqualTo("collectionsindex-v1");
        assertThat(request.source().query().toString()).contains("tenantId");
    }

    @Test
    void buildSearchRequest_missingMust_throwsIllegalArgumentException() throws IOException {
        ObjectNode query = new ObjectMapper().readValue("{\"query\":{\"bool\":{}}}", ObjectNode.class);

        assertThatThrownBy(() -> service.buildSearchRequest("idx", query))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
