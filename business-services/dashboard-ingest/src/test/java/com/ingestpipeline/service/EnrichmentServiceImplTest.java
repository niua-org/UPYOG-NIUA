package com.ingestpipeline.service;

import com.ingestpipeline.config.DomainConfigFactory;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceImplTest {

    @Mock
    private JSONUtil jsonUtil;
    @Mock
    private DomainConfigFactory domainConfigFactory;

    private EnrichmentServiceImpl enrichmentService;

    @BeforeEach
    void setUp() {
        enrichmentService = new EnrichmentServiceImpl(
                "http://localhost:9200/", "user", "pass", "idx", "doc", jsonUtil);
        org.springframework.test.util.ReflectionTestUtils.setField(enrichmentService, "domainConfigFactory", domainConfigFactory);
    }

    @Test
    void enrichData_collectionContextWithoutDomainConfig_returnsIncomingPayload() {
        Map<String, Object> incoming = new HashMap<>();
        incoming.put("dataContext", "collection");
        incoming.put("dataObject", Map.of("tenantId", "pb.test", "businessService", "PT"));

        when(jsonUtil.fetchMCollectCategories(anyString())).thenReturn(List.of());
        when(domainConfigFactory.getConfiguration("collection")).thenReturn(null);

        Map<?, ?> result = enrichmentService.enrichData(incoming);

        assertThat(result).isSameAs(incoming);
    }
}
