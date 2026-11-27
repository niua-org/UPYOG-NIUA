package org.egov.infra.mdms.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsRequest;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeUniqueIdentifierGenerationUtilTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getJsonPointerExpressionFromDotSeparatedPath_convertsDotsToSlashes() {
        assertThat(CompositeUniqueIdentifierGenerationUtil.getJsonPointerExpressionFromDotSeparatedPath("a.b.c"))
                .isEqualTo("/a/b/c");
    }

    @Test
    void getUniqueIdentifier_buildsDotSeparatedCompositeKey() throws Exception {
        JSONObject schema = new JSONObject();
        schema.put(MDMSConstants.X_UNIQUE_KEY, new org.json.JSONArray(List.of("code", "tenantId")));
        MdmsRequest request = MdmsRequest.builder()
                .mdms(Mdms.builder()
                        .data(objectMapper.readTree("{\"code\":\"PT\",\"tenantId\":\"pg.citya\"}"))
                        .build())
                .build();

        assertThat(CompositeUniqueIdentifierGenerationUtil.getUniqueIdentifier(schema, request))
                .isEqualTo("PT.pg.citya");
    }

    @Test
    void getUniqueIdentifier_emptyUniqueField_throwsCustomException() throws Exception {
        JSONObject schema = new JSONObject();
        schema.put(MDMSConstants.X_UNIQUE_KEY, new org.json.JSONArray(List.of("code")));
        MdmsRequest request = MdmsRequest.builder()
                .mdms(Mdms.builder()
                        .data(objectMapper.readTree("{\"code\":\"\"}"))
                        .build())
                .build();

        assertThatThrownBy(() -> CompositeUniqueIdentifierGenerationUtil.getUniqueIdentifier(schema, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "UNIQUE_IDENTIFIER_EMPTY_ERR");
    }
}
