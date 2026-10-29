package org.egov.inbox.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ElasticSearchQueryBuilderTest {

    @Mock
    private InboxConfiguration config;
    @Mock
    private EncryptionService encryptionService;

    private ObjectMapper mapper;
    private ElasticSearchQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        queryBuilder = new ElasticSearchQueryBuilder(mapper, config, encryptionService);
    }

    @Test
    void merge_whenUpdateAddsField_replacesValueOnMainNode() {
        ObjectNode main = mapper.createObjectNode().put("tenantId", "pb.amritsar");
        ObjectNode update = mapper.createObjectNode().put("tenantId", "pb.jalandhar");

        ElasticSearchQueryBuilder.merge(main, update);

        assertThat(main.get("tenantId").asText()).isEqualTo("pb.jalandhar");
    }

    @Test
    void merge_whenUpdateAddsNewField_appendsToMain() {
        ObjectNode main = mapper.createObjectNode().put("status", "OPEN");
        ObjectNode update = mapper.createObjectNode().put("module", "WS");

        ElasticSearchQueryBuilder.merge(main, update);

        assertThat(main.get("module").asText()).isEqualTo("WS");
        assertThat(main.get("status").asText()).isEqualTo("OPEN");
    }
}
