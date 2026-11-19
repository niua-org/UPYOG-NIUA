package org.egov.infra.mdms.service.enrichment;

import org.egov.common.contract.models.AuditDetails;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsRequest;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmsDataEnricherTest {

    @InjectMocks
    private MdmsDataEnricher enricher;

    @Test
    void enrichUpdateRequest_missingAuditDetails_throwsAuditDetailsAbsent() {
        Mdms mdms = mock(Mdms.class);
        when(mdms.getAuditDetails()).thenReturn(null);
        MdmsRequest request = mock(MdmsRequest.class);
        when(request.getMdms()).thenReturn(mdms);

        assertThatThrownBy(() -> enricher.enrichUpdateRequest(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "AUDIT_DETAILS_ABSENT_ERR");
    }

    @Test
    void getAuditDetails_createRequest_setsCreatedAndModifiedByUser() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("user-1").build())
                .build();

        AuditDetails audit = enricher.getAuditDetails(requestInfo, null, true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isPositive();
        assertThat(audit.getLastModifiedTime()).isPositive();
    }

    @Test
    void getAuditDetails_updateRequest_preservesCreatedFieldsAndUpdatesModifier() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("editor").build())
                .build();
        AuditDetails existing = AuditDetails.builder()
                .createdBy("creator")
                .createdTime(100L)
                .lastModifiedBy("creator")
                .lastModifiedTime(100L)
                .build();

        AuditDetails audit = enricher.getAuditDetails(requestInfo, existing, false);

        assertThat(audit.getCreatedBy()).isEqualTo("creator");
        assertThat(audit.getCreatedTime()).isEqualTo(100L);
        assertThat(audit.getLastModifiedBy()).isEqualTo("editor");
        assertThat(audit.getLastModifiedTime()).isGreaterThan(100L);
    }

    @Test
    void getAuditDetails_updateRequestNullAudit_throwsCustomException() {
        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("editor").build())
                .build();

        assertThatThrownBy(() -> enricher.getAuditDetails(requestInfo, null, false))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "AUDIT_DETAILS_NULL_FOR_UPDATE_REQ");
    }

    @Test
    void enrichCreateRequest_validPayload_setsUniqueIdentifierFromSchema() {
        JSONObject schema = new JSONObject();
        schema.put("x-unique", new org.json.JSONArray().put("code"));
        Mdms mdms = Mdms.builder()
                .tenantId("pg.citya")
                .schemaCode("common-masters.Department")
                .data(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("code", "A"))
                .build();
        MdmsRequest request = MdmsRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("u1").build()).build())
                .mdms(mdms)
                .build();

        enricher.enrichCreateRequest(request, schema);

        assertThat(mdms.getId()).isNotBlank();
        assertThat(mdms.getUniqueIdentifier()).isNotBlank();
        assertThat(mdms.getAuditDetails().getCreatedBy()).isEqualTo("u1");
    }
}
