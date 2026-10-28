package org.egov.infra.indexer.validator;

import org.egov.IndexerApplicationRunnerImpl;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.infra.indexer.bulkindexer.BulkIndexer;
import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.infra.indexer.web.contract.LegacyIndexRequest;
import org.egov.infra.indexer.web.contract.Mapping;
import org.egov.infra.indexer.web.contract.ReindexRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatorTest {

    @Mock
    private IndexerApplicationRunnerImpl runner;
    @Mock
    private IndexerUtils indexerUtils;
    @Mock
    private BulkIndexer bulkIndexer;

    @InjectMocks
    private Validator validator;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("admin").roles(List.of()).build())
                .build();
    }

    @Test
    void validaterReindexRequest_unknownTopic_throwsMissingConfig() {
        ReindexRequest request = ReindexRequest.builder()
                .requestInfo(requestInfo)
                .reindexTopic("unknown-topic")
                .build();
        when(runner.getMappingMaps()).thenReturn(new ConcurrentHashMap<>());

        assertThatThrownBy(() -> validator.validaterReindexRequest(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "EG_INDEXER_MISSING_CONFIG");
    }

    @Test
    void validaterReindexRequest_multipleIndexes_throwsInvalidReindexConfig() {
        ReindexRequest request = ReindexRequest.builder()
                .requestInfo(requestInfo)
                .reindexTopic("known-topic")
                .build();
        Mapping mapping = new Mapping();
        mapping.setIndexes(List.of(new Index(), new Index()));
        ConcurrentHashMap<String, Mapping> maps = new ConcurrentHashMap<>();
        maps.put("known-topic", mapping);
        when(runner.getMappingMaps()).thenReturn(maps);

        assertThatThrownBy(() -> validator.validaterReindexRequest(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "EG_INDEXER_INVALID_REINDEX_CONFIG");
    }

    @Test
    void validaterLegacyindexRequest_unknownTopic_throwsMissingConfig() {
        LegacyIndexRequest request = LegacyIndexRequest.builder()
                .requestInfo(requestInfo)
                .legacyIndexTopic("missing")
                .build();
        when(runner.getMappingMaps()).thenReturn(new ConcurrentHashMap<>());

        assertThatThrownBy(() -> validator.validaterLegacyindexRequest(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "EG_INDEXER_MISSING_CONFIG");
    }
}
