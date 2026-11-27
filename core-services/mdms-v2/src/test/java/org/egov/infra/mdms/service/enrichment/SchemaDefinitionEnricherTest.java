package org.egov.infra.mdms.service.enrichment;

import org.egov.common.contract.models.AuditDetails;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.infra.mdms.model.SchemaDefinition;
import org.egov.infra.mdms.model.SchemaDefinitionRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaDefinitionEnricherTest {

    private final SchemaDefinitionEnricher enricher = new SchemaDefinitionEnricher();

    @Test
    void enrichCreateRequest_setsIdAndAuditDetails() {
        SchemaDefinition definition = SchemaDefinition.builder().tenantId("pg").code("common.StateInfo").build();
        SchemaDefinitionRequest request = SchemaDefinitionRequest.builder()
                .requestInfo(RequestInfo.builder()
                        .userInfo(User.builder().uuid("user-1").build())
                        .build())
                .schemaDefinition(definition)
                .build();

        enricher.enrichCreateRequest(request);

        assertThat(definition.getId()).isNotBlank();
        assertThat(definition.getAuditDetails().getCreatedBy()).isEqualTo("user-1");
        assertThat(definition.getAuditDetails().getLastModifiedBy()).isEqualTo("user-1");
    }

    @Test
    void getAuditDetail_updateWithoutExistingAudit_throws() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("user-1").build())
                .build();

        assertThatThrownBy(() -> enricher.getAuditDetail(requestInfo, null, false))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void getAuditDetail_updateWithExistingAudit_preservesCreatedFields() {
        AuditDetails existing = AuditDetails.builder()
                .createdBy("creator")
                .createdTime(100L)
                .build();
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("editor").build())
                .build();

        AuditDetails updated = enricher.getAuditDetail(requestInfo, existing, false);

        assertThat(updated.getCreatedBy()).isEqualTo("creator");
        assertThat(updated.getCreatedTime()).isEqualTo(100L);
        assertThat(updated.getLastModifiedBy()).isEqualTo("editor");
    }
}
