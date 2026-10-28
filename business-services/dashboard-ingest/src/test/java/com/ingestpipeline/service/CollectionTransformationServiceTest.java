package com.ingestpipeline.service;

import com.ingestpipeline.util.ConfigLoader;
import com.ingestpipeline.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionTransformationServiceTest {

    @Mock
    private ConfigLoader configLoader;

    @InjectMocks
    private CollectionTransformationService collectionTransformationService;

    private String collectionTransformSpec;

    @BeforeEach
    void loadSpecFromClasspath() throws Exception {
        collectionTransformSpec = new ClassPathResource("config/transform_collection_v1.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void transformData_splitsNestedBillDetailsIntoKeyedOutputs() {
        when(configLoader.get(eq("transform_collection_v1.json"))).thenReturn(collectionTransformSpec);

        Map<String, Object> billDetail = new HashMap<>();
        billDetail.put("id", "line-1");
        billDetail.put("consumerCode", "CC-100");
        billDetail.put("businessService", "PT");

        Map<String, Object> bill = Map.of(
                "billDetails", List.of(billDetail),
                "id", "bill-1",
                "tenantId", "pb.test"
        );

        Map<String, Object> incoming = new HashMap<>();
        incoming.put(Constants.DATA_CONTEXT, "collection");
        incoming.put(Constants.DATA_CONTEXT_VERSION, "v1");
        incoming.put(Constants.DATA_OBJECT, Map.of(
                "tenantId", "pb.test",
                "transactionId", "tx-42",
                "Bill", List.of(bill)
        ));

        Boolean transformed = collectionTransformationService.transformData(incoming);

        assertThat(transformed).isTrue();
        assertThat(incoming).containsKey("line-1");
        Map<?, ?> transformedEntry = (Map<?, ?>) incoming.get("line-1");
        assertThat(transformedEntry.get(Constants.IDENTIFIER)).isEqualTo("line-1");
        assertThat(transformedEntry.get(Constants.DATA_CONTEXT)).isEqualTo("collection");
    }

    @Test
    void transformData_missingSpec_returnsFalse() {
        when(configLoader.get(eq("transform_collection_v1.json"))).thenThrow(new RuntimeException("missing"));

        Map<String, Object> incoming = new HashMap<>();
        incoming.put(Constants.DATA_CONTEXT, "collection");
        incoming.put(Constants.DATA_CONTEXT_VERSION, "v1");
        incoming.put(Constants.DATA_OBJECT, Map.of("tenantId", "pb"));

        assertThat(collectionTransformationService.transformData(incoming)).isFalse();
    }
}
