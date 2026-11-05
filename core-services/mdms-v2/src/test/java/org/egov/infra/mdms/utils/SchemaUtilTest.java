package org.egov.infra.mdms.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsRequest;
import org.egov.infra.mdms.model.SchemaDefSearchRequest;
import org.egov.infra.mdms.model.SchemaDefinition;
import org.egov.infra.mdms.service.SchemaDefinitionService;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaUtilTest {

    @Mock
    private SchemaDefinitionService schemaDefinitionService;

    private SchemaUtil schemaUtil;

    @BeforeEach
    void setUp() {
        schemaUtil = new SchemaUtil(schemaDefinitionService);
    }

    @Test
    void getSchema_found_returnsJsonObject() throws Exception {
        MdmsRequest request = MdmsRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .mdms(Mdms.builder().tenantId("pg").schemaCode("common.StateInfo").build())
                .build();
        when(schemaDefinitionService.search(any(SchemaDefSearchRequest.class)))
                .thenReturn(List.of(SchemaDefinition.builder()
                        .definition(new ObjectMapper().readTree("{\"type\":\"object\"}"))
                        .build()));

        JSONObject schema = schemaUtil.getSchema(request);

        assertThat(schema.get("type")).isEqualTo("object");
    }

    @Test
    void getSchema_notFound_throwsCustomException() {
        MdmsRequest request = MdmsRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .mdms(Mdms.builder().tenantId("pg").schemaCode("missing").build())
                .build();
        when(schemaDefinitionService.search(any(SchemaDefSearchRequest.class))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> schemaUtil.getSchema(request))
                .isInstanceOf(CustomException.class);
    }
}
