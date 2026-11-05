package org.egov.infra.mdms.utils;

import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsResponseV2;
import org.egov.infra.mdms.model.SchemaDefinition;
import org.egov.infra.mdms.model.SchemaDefinitionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseUtilTest {

    @Test
    void getSchemaDefinitionResponse_wrapsDefinitionsAndTotal() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("mdms").build();
        List<SchemaDefinition> definitions = List.of(SchemaDefinition.builder().code("City").build());

        SchemaDefinitionResponse response =
                ResponseUtil.getSchemaDefinitionResponse(requestInfo, definitions, 3);

        assertThat(response.getSchemaDefinitions()).isEqualTo(definitions);
        assertThat(response.getTotalMasters()).isEqualTo(3);
        assertThat(response.getResponseInfo().getStatus()).isEqualTo("successful");
    }

    @Test
    void getMasterDataV2Response_wrapsMdmsList() {
        RequestInfo requestInfo = RequestInfo.builder().apiId("mdms").build();
        List<Mdms> masters = List.of(Mdms.builder().tenantId("pg.citya").schemaCode("City").build());

        MdmsResponseV2 response = ResponseUtil.getMasterDataV2Response(requestInfo, masters);

        assertThat(response.getMdms()).isEqualTo(masters);
        assertThat(response.getResponseInfo().getApiId()).isEqualTo("mdms");
    }
}
