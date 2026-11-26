package com.tarento.analytics.utils;

import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticSearchClientTest {

    @Test
    void getClient_buildsRestHighLevelClient() {
        ElasticSearchClient client = new ElasticSearchClient(
                "localhost", 9200, 5, "transactions", "doc", "targets", "target-doc");

        try (RestHighLevelClient esClient = client.getClient()) {
            assertThat(esClient).isNotNull();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
