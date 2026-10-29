package com.ingestpipeline.service;

import com.ingestpipeline.util.ConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichTransformTest {

    @Mock
    private ConfigLoader configLoader;

    @InjectMocks
    private EnrichTransform enrichTransform;

    private String ptTransformSpec;

    @BeforeEach
    void loadSpec() throws Exception {
        ptTransformSpec = new ClassPathResource("config/transform_pt_v1.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void transform_mapsElasticSourceIntoDomainShape() throws Exception {
        when(configLoader.get(eq("transform_pt_v1.json"))).thenReturn(ptTransformSpec);

        Map<String, Object> esHit = Map.of(
                "_source", Map.of(
                        "Data", Map.of(
                                "tenantId", "pb.test",
                                "propertyId", "prop-1",
                                "propertyDetails", Map.of("assessmentNumber", "AS-1"),
                                "ward", "W-1"
                        )
                )
        );

        Object transformed = enrichTransform.transform(esHit, "PT");

        assertThat(transformed).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) transformed).get("propertyId")).isEqualTo("prop-1");
    }

    @Test
    void transformEnhanceData_processesListOfDomainDocuments() throws Exception {
        String enhanceSpec = new ClassPathResource("config/transform_pt_enhance_v1.json")
                .getContentAsString(StandardCharsets.UTF_8);
        when(configLoader.get(eq("transform_pt_enhance_v1.json"))).thenReturn(enhanceSpec);

        List<Map<String, Object>> docs = List.of(
                Map.of("Data", Map.of("tenantId", "pb.test", "propertyId", "prop-9"))
        );

        Object transformed = enrichTransform.transformEnhanceData(docs, "PT");

        assertThat(transformed).isInstanceOf(List.class);
        assertThat((List<?>) transformed).isNotEmpty();
    }
}
