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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Test
    void validateUserRBACProxy_missingUuid_throws() {
        RequestInfo noUuid = RequestInfo.builder()
                .userInfo(User.builder().roles(List.of()).build())
                .build();

        assertThatThrownBy(() -> validator.validateUserRBACProxy(new java.util.HashMap<>(), noUuid))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validaterReindexRequest_validConfigAndData_passes() {
        ReindexRequest request = ReindexRequest.builder()
                .requestInfo(requestInfo)
                .reindexTopic("known-topic")
                .index("idx")
                .type("typ")
                .build();
        Mapping mapping = new Mapping();
        mapping.setIndexes(List.of(new Index()));
        ConcurrentHashMap<String, Mapping> maps = new ConcurrentHashMap<>();
        maps.put("known-topic", mapping);
        when(runner.getMappingMaps()).thenReturn(maps);
        when(indexerUtils.getESSearchURL(request)).thenReturn("http://es/idx/typ/_search");
        when(bulkIndexer.getESResponse(any(), isNull(), isNull()))
                .thenReturn(Map.of("hits", Map.of("total", 5)));

        assertThatCode(() -> validator.validaterReindexRequest(request)).doesNotThrowAnyException();
    }

    @Test
    void validaterReindexRequest_noEsResponse_throwsInvalidIndex() {
        ReindexRequest request = ReindexRequest.builder()
                .requestInfo(requestInfo)
                .reindexTopic("known-topic")
                .build();
        Mapping mapping = new Mapping();
        mapping.setIndexes(List.of(new Index()));
        ConcurrentHashMap<String, Mapping> maps = new ConcurrentHashMap<>();
        maps.put("known-topic", mapping);
        when(runner.getMappingMaps()).thenReturn(maps);
        when(indexerUtils.getESSearchURL(request)).thenReturn("http://es/_search");
        when(bulkIndexer.getESResponse(any(), isNull(), isNull())).thenReturn(null);

        assertThatThrownBy(() -> validator.validaterReindexRequest(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "EG_INDEXER_INAVLID_INDEX");
    }

    @Test
    void validaterReindexRequest_zeroHits_throwsInvalidIndex() {
        ReindexRequest request = ReindexRequest.builder()
                .requestInfo(requestInfo)
                .reindexTopic("known-topic")
                .build();
        Mapping mapping = new Mapping();
        mapping.setIndexes(List.of(new Index()));
        ConcurrentHashMap<String, Mapping> maps = new ConcurrentHashMap<>();
        maps.put("known-topic", mapping);
        when(runner.getMappingMaps()).thenReturn(maps);
        when(indexerUtils.getESSearchURL(request)).thenReturn("http://es/_search");
        when(bulkIndexer.getESResponse(any(), isNull(), isNull()))
                .thenReturn(Map.of("hits", Map.of("total", 0)));

        assertThatThrownBy(() -> validator.validaterReindexRequest(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "EG_INDEXER_INAVLID_INDEX");
    }

    @Test
    void validaterLegacyindexRequest_multipleIndexes_throwsInvalidConfig() {
        LegacyIndexRequest request = LegacyIndexRequest.builder()
                .requestInfo(requestInfo)
                .legacyIndexTopic("legacy-topic")
                .build();
        Mapping mapping = new Mapping();
        mapping.setIndexes(List.of(new Index(), new Index()));
        ConcurrentHashMap<String, Mapping> maps = new ConcurrentHashMap<>();
        maps.put("legacy-topic", mapping);
        when(runner.getMappingMaps()).thenReturn(maps);

        assertThatThrownBy(() -> validator.validaterLegacyindexRequest(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "EG_INDEXER_INVALID_REINDEX_CONFIG");
    }

    @Test
    void validaterLegacyindexRequest_singleIndex_passes() {
        LegacyIndexRequest request = LegacyIndexRequest.builder()
                .requestInfo(requestInfo)
                .legacyIndexTopic("legacy-topic")
                .build();
        Mapping mapping = new Mapping();
        mapping.setIndexes(List.of(new Index()));
        ConcurrentHashMap<String, Mapping> maps = new ConcurrentHashMap<>();
        maps.put("legacy-topic", mapping);
        when(runner.getMappingMaps()).thenReturn(maps);

        assertThatCode(() -> validator.validaterLegacyindexRequest(request)).doesNotThrowAnyException();
    }
}
