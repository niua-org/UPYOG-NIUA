package org.egov.infra.mdms.service;

import org.egov.common.contract.models.AuditDetails;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsCriteriaReqV2;
import org.egov.infra.mdms.model.MdmsCriteriaV2;
import org.egov.infra.mdms.model.MdmsRequest;
import org.egov.infra.mdms.model.Operation;
import org.egov.infra.mdms.repository.MdmsDataRepository;
import org.egov.infra.mdms.service.enrichment.MdmsDataEnricher;
import org.egov.infra.mdms.service.validator.MdmsDataValidator;
import org.egov.infra.mdms.utils.SchemaUtil;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MDMSServiceV2Test {

    @Mock
    private MdmsDataValidator mdmsDataValidator;

    @Mock
    private MdmsDataEnricher mdmsDataEnricher;

    @Mock
    private MdmsDataRepository mdmsDataRepository;

    @Mock
    private SchemaUtil schemaUtil;

    @Mock
    private MultiStateInstanceUtil multiStateInstanceUtil;

    @InjectMocks
    private MDMSServiceV2 mdmsServiceV2;

    @Test
    void create_validRequest_setsOperationAndPersists() {
        Mdms mdms = mock(Mdms.class);
        MdmsRequest request = mock(MdmsRequest.class);
        JSONObject schema = new JSONObject();
        when(request.getMdms()).thenReturn(mdms);
        when(schemaUtil.getSchema(request)).thenReturn(schema);

        List<Mdms> created = mdmsServiceV2.create(request);

        assertThat(created).containsExactly(mdms);
        verify(mdms).setOperation(Operation.CREATE.name());
        InOrder order = inOrder(mdmsDataValidator, mdmsDataEnricher, mdmsDataRepository);
        order.verify(mdmsDataValidator).validateCreateRequest(request, schema);
        order.verify(mdmsDataEnricher).enrichCreateRequest(request, schema);
        order.verify(mdmsDataRepository).create(request);
    }

    @Test
    void create_validatorThrows_doesNotPersist() {
        MdmsRequest request = mock(MdmsRequest.class);
        JSONObject schema = new JSONObject();
        when(schemaUtil.getSchema(request)).thenReturn(schema);
        doThrow(new CustomException("DUPLICATE_RECORD", "Duplicate record"))
                .when(mdmsDataValidator).validateCreateRequest(request, schema);

        assertThatThrownBy(() -> mdmsServiceV2.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Duplicate record");
        verify(mdmsDataEnricher, never()).enrichCreateRequest(any(), any());
        verify(mdmsDataRepository, never()).create(any());
    }

    @Test
    void search_firstTenantHasData_returnsWithoutFallback() {
        MdmsCriteriaV2 criteria = MdmsCriteriaV2.builder().tenantId("pg.citya").build();
        MdmsCriteriaReqV2 request = MdmsCriteriaReqV2.builder().mdmsCriteria(criteria).build();
        Mdms row = mock(Mdms.class);
        when(mdmsDataRepository.searchV2(criteria)).thenReturn(List.of(row));

        List<Mdms> result = mdmsServiceV2.search(request);

        assertThat(result).containsExactly(row);
        verify(mdmsDataRepository).searchV2(criteria);
    }

    @Test
    void search_fallsBackToParentTenant_whenChildEmpty() {
        MdmsCriteriaV2 criteria = MdmsCriteriaV2.builder().tenantId("pg.citya").build();
        MdmsCriteriaReqV2 request = MdmsCriteriaReqV2.builder().mdmsCriteria(criteria).build();
        Mdms row = mock(Mdms.class);
        when(mdmsDataRepository.searchV2(any(MdmsCriteriaV2.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(row));

        List<Mdms> result = mdmsServiceV2.search(request);

        assertThat(result).containsExactly(row);
        assertThat(criteria.getTenantId()).isEqualTo("pg");
    }

    @Test
    void search_allFallbackTenantsEmpty_returnsEmptyList() {
        MdmsCriteriaV2 criteria = MdmsCriteriaV2.builder().tenantId("pg.citya").build();
        MdmsCriteriaReqV2 request = MdmsCriteriaReqV2.builder().mdmsCriteria(criteria).build();
        when(mdmsDataRepository.searchV2(any(MdmsCriteriaV2.class))).thenReturn(Collections.emptyList());

        assertThat(mdmsServiceV2.search(request)).isEmpty();
    }

    @Test
    void delete_existingRecord_publishesDelete() {
        Mdms mdms = Mdms.builder().id("id-1").tenantId("pg.citya").build();
        MdmsRequest request = MdmsRequest.builder().mdms(mdms).build();
        Mdms existing = Mdms.builder().id("id-1").tenantId("pg.citya").build();
        when(multiStateInstanceUtil.getStateLevelTenant("pg.citya")).thenReturn("pg");
        when(mdmsDataRepository.getById("pg", "id-1")).thenReturn(existing);

        List<Mdms> deleted = mdmsServiceV2.delete(request, "schema.code");

        assertThat(deleted).containsExactly(mdms);
        assertThat(mdms.getTenantId()).isEqualTo("pg");
        assertThat(mdms.getOperation()).isEqualTo(Operation.DELETE.name());
        verify(mdmsDataRepository).delete(request);
    }

    @Test
    void delete_missingRecord_throwsRuntimeException() {
        Mdms mdms = Mdms.builder().id("missing").tenantId("pg.citya").build();
        MdmsRequest request = MdmsRequest.builder().mdms(mdms).build();
        when(multiStateInstanceUtil.getStateLevelTenant("pg.citya")).thenReturn("pg");
        when(mdmsDataRepository.getById("pg", "missing")).thenReturn(null);

        assertThatThrownBy(() -> mdmsServiceV2.delete(request, "schema.code"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Master data not found");
        verify(mdmsDataRepository, never()).delete(any());
    }

    @Test
    void update_existingRecord_copiesAuditFieldsAndPersists() {
        AuditDetails existingAudit = AuditDetails.builder()
                .createdBy("creator")
                .createdTime(100L)
                .build();
        AuditDetails requestAudit = AuditDetails.builder().build();
        Mdms mdms = Mdms.builder()
                .id("id-1")
                .tenantId("pg.citya")
                .auditDetails(requestAudit)
                .build();
        MdmsRequest request = MdmsRequest.builder().mdms(mdms).build();
        Mdms existing = Mdms.builder()
                .id("id-1")
                .tenantId("pg")
                .schemaCode("common-masters.Department")
                .uniqueIdentifier("DEPT-1")
                .auditDetails(existingAudit)
                .build();
        JSONObject schema = new JSONObject();
        when(schemaUtil.getSchema(request)).thenReturn(schema);
        when(mdmsDataRepository.getById("pg.citya", "id-1")).thenReturn(existing);

        List<Mdms> updated = mdmsServiceV2.update(request);

        assertThat(updated).containsExactly(mdms);
        assertThat(mdms.getTenantId()).isEqualTo("pg");
        assertThat(mdms.getSchemaCode()).isEqualTo("common-masters.Department");
        assertThat(mdms.getUniqueIdentifier()).isEqualTo("DEPT-1");
        assertThat(mdms.getOperation()).isEqualTo(Operation.UPDATE.name());
        assertThat(requestAudit.getCreatedBy()).isEqualTo("creator");
        assertThat(requestAudit.getCreatedTime()).isEqualTo(100L);
        InOrder order = inOrder(mdmsDataValidator, mdmsDataEnricher, mdmsDataRepository);
        order.verify(mdmsDataValidator).validateUpdateRequest(request, schema);
        order.verify(mdmsDataEnricher).enrichUpdateRequest(request);
        order.verify(mdmsDataRepository).update(request);
    }

    @Test
    void update_missingRecord_throwsRuntimeException() {
        Mdms mdms = Mdms.builder().id("missing").tenantId("pg").auditDetails(AuditDetails.builder().build()).build();
        MdmsRequest request = MdmsRequest.builder().mdms(mdms).build();
        when(schemaUtil.getSchema(request)).thenReturn(new JSONObject());
        when(mdmsDataRepository.getById("pg", "missing")).thenReturn(null);

        assertThatThrownBy(() -> mdmsServiceV2.update(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Master data not found");
        verify(mdmsDataRepository, never()).update(any());
    }
}
