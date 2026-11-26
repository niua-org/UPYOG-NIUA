package com.ingestpipeline.service;

import com.ingestpipeline.config.DomainConfigFactory;
import com.ingestpipeline.configfactory.CollectionDomainConfig;
import com.ingestpipeline.configfactory.EnhanceDomainConfig;
import com.ingestpipeline.model.DomainIndexConfig;
import com.ingestpipeline.model.SourceReferences;
import com.ingestpipeline.model.TargetReferences;
import com.ingestpipeline.util.JSONUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceImplTest {

    @Mock
    private JSONUtil jsonUtil;
    @Mock
    private DomainConfigFactory domainConfigFactory;
    @Mock
    private IESService elasticService;
    @Mock
    private EnrichTransform enrichTransform;

    private CollectionDomainConfig collectionDomainConfig;
    private EnhanceDomainConfig enhanceDomainConfig;
    private EnrichmentServiceImpl enrichmentService;

    @BeforeEach
    void setUp() {
        collectionDomainConfig = new CollectionDomainConfig();
        enhanceDomainConfig = new EnhanceDomainConfig();
        enrichmentService = new EnrichmentServiceImpl(
                "http://localhost:9200/", "user", "pass", "idx", "doc", jsonUtil);
        org.springframework.test.util.ReflectionTestUtils.setField(enrichmentService, "domainConfigFactory", domainConfigFactory);
        org.springframework.test.util.ReflectionTestUtils.setField(enrichmentService, "elasticService", elasticService);
        org.springframework.test.util.ReflectionTestUtils.setField(enrichmentService, "enrichTransform", enrichTransform);
    }

    @Test
    void enrichData_collectionContextWithoutIndexConfig_returnsIncomingPayload() {
        Map<String, Object> incoming = baseCollectionPayload("PT");
        when(jsonUtil.fetchMCollectCategories(anyString())).thenReturn(List.of());
        when(domainConfigFactory.getConfiguration("collection")).thenReturn(collectionDomainConfig);

        Map<?, ?> result = enrichmentService.enrichData(incoming);

        assertThat(result).isSameAs(incoming);
    }

    @Test
    void enrichData_collectionContextWithEsHit_enrichesDomainObject() throws Exception {
        Map<String, Object> incoming = baseCollectionPayload("PT");
        DomainIndexConfig indexConfig = collectionIndexConfig();
        collectionDomainConfig.putDomain("MCOLLECT", indexConfig);

        when(jsonUtil.fetchMCollectCategories(anyString())).thenReturn(List.of("PT"));
        when(domainConfigFactory.getConfiguration("collection")).thenReturn(collectionDomainConfig);
        when(elasticService.search(eq("pt-index"), anyString()))
                .thenReturn(Map.of("_source", Map.of("Data", Map.of("propertyId", "p1"))));
        when(enrichTransform.transform(org.mockito.ArgumentMatchers.anyMap(), eq("MCOLLECT")))
                .thenReturn(Map.of("propertyId", "p1"));
        when(domainConfigFactory.getConfiguration("dataEnhancement")).thenReturn(enhanceDomainConfig);

        Map<?, ?> result = enrichmentService.enrichData(incoming);

        assertThat(result.get("domainObject")).isEqualTo(Map.of("propertyId", "p1"));
        verify(elasticService).search(eq("pt-index"), anyString());
    }

    @Test
    void enrichData_collectionHit_triggersEnhancementLookup() throws Exception {
        Map<String, Object> incoming = baseCollectionPayload("PT");
        DomainIndexConfig indexConfig = collectionIndexConfig();
        collectionDomainConfig.putDomain("MCOLLECT", indexConfig);

        DomainIndexConfig enhanceIndex = collectionIndexConfig();
        enhanceIndex.setIndexName("pt-enhance-index");
        enhanceDomainConfig.putDomain("PT", enhanceIndex);

        when(jsonUtil.fetchMCollectCategories(anyString())).thenReturn(List.of("PT"));
        when(domainConfigFactory.getConfiguration("collection")).thenReturn(collectionDomainConfig);
        when(domainConfigFactory.getConfiguration("dataEnhancement")).thenReturn(enhanceDomainConfig);
        when(elasticService.search(eq("pt-index"), anyString()))
                .thenReturn(Map.of("_source", Map.of("Data", Map.of("propertyId", "p1"))));
        when(elasticService.searchMultiple(eq("pt-enhance-index"), anyString()))
                .thenReturn(List.of(Map.of("_source", Map.of("Data", Map.of("assessment", "A-1")))));
        when(enrichTransform.transform(org.mockito.ArgumentMatchers.anyMap(), eq("MCOLLECT")))
                .thenReturn(new HashMap<>(Map.of("propertyId", "p1")));
        when(enrichTransform.transformEnhanceData(org.mockito.ArgumentMatchers.anyList(), eq("PT")))
                .thenReturn(List.of(Map.of("assessment", "A-1")));

        Map<?, ?> result = enrichmentService.enrichData(incoming);

        assertThat(result.get("domainObject")).isInstanceOf(Map.class);
        verify(elasticService).searchMultiple(eq("pt-enhance-index"), anyString());
    }

    private static Map<String, Object> baseCollectionPayload(String businessService) {
        Map<String, Object> dataObject = new HashMap<>();
        dataObject.put("tenantId", "pb.test");
        dataObject.put("businessService", businessService);
        dataObject.put("consumerCode", "CC-001");

        Map<String, Object> incoming = new HashMap<>();
        incoming.put("dataContext", "collection");
        incoming.put("dataObject", dataObject);
        return incoming;
    }

    private static DomainIndexConfig collectionIndexConfig() {
        SourceReferences source = new SourceReferences();
        source.setFieldName("consumerCode");
        source.setExpression("code");
        source.setSeperator("-");

        TargetReferences target = new TargetReferences();
        target.setArgument("query.bool.must[0].match_phrase.code");
        target.setExpression("code");
        target.setSeperator("-");

        DomainIndexConfig indexConfig = new DomainIndexConfig();
        indexConfig.setIndexName("pt-index");
        indexConfig.setQuery("""
                {"query":{"bool":{"must":[{"match_phrase":{"code":"value"}}]}}}
                """);
        indexConfig.setSourceReferences(List.of(source));
        indexConfig.setTargetReferences(List.of(target));
        return indexConfig;
    }
}
