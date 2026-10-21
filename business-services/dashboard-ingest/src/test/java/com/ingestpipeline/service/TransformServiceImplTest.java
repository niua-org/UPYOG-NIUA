package com.ingestpipeline.service;

import com.ingestpipeline.util.Constants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransformServiceImplTest {

    private final TransformServiceImpl transformService = new TransformServiceImpl();

    @Test
    void transformData_appliesJoltSpecForTargetV1() {
        Map<String, Object> row = Map.of(
                "S. No.", "1",
                "Tenant Id", "pb.test",
                "ULB Name", "Amritsar",
                "Financial Year", "2023-24",
                "Sheet Name", "PT",
                "Timestamp", "2024-01-01"
        );
        Map<String, Object> incoming = new HashMap<>();
        incoming.put(Constants.DATA_CONTEXT, "target");
        incoming.put(Constants.DATA_CONTEXT_VERSION, "v1");
        incoming.put(Constants.DATA_OBJECT, List.of(row));

        Boolean transformed = transformService.transformData(incoming);

        assertThat(transformed).isTrue();
        assertThat(incoming.get(Constants.DATA_OBJECT)).isNotNull();
    }

    @Test
    void transformData_missingSpec_throwsFromJoltChainr() {
        Map<String, Object> incoming = new HashMap<>();
        incoming.put(Constants.DATA_CONTEXT, "unknown");
        incoming.put(Constants.DATA_CONTEXT_VERSION, "v0");
        incoming.put(Constants.DATA_OBJECT, Map.of("a", 1));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> transformService.transformData(incoming));
    }
}
