package org.egov.infra.mdms.service;

import net.minidev.json.JSONArray;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.infra.mdms.model.MasterDetail;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsCriteria;
import org.egov.infra.mdms.model.MdmsCriteriaReq;
import org.egov.infra.mdms.model.MdmsRequest;
import org.egov.infra.mdms.model.ModuleDetail;
import org.egov.infra.mdms.repository.MdmsDataRepository;
import org.egov.infra.mdms.service.enrichment.MdmsDataEnricher;
import org.egov.infra.mdms.service.validator.MdmsDataValidator;
import org.egov.infra.mdms.utils.SchemaUtil;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MDMSServiceTest {

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
    private MDMSService mdmsService;

    @Test
    void create_validRequest_returnsMdms() {
        Mdms mdms = mock(Mdms.class);
        MdmsRequest request = mock(MdmsRequest.class);
        JSONObject schema = new JSONObject();
        when(request.getMdms()).thenReturn(mdms);
        when(schemaUtil.getSchema(request)).thenReturn(schema);

        List<Mdms> created = mdmsService.create(request);

        assertThat(created).containsExactly(mdms);
        InOrder order = inOrder(mdmsDataValidator, mdmsDataEnricher, mdmsDataRepository);
        order.verify(mdmsDataValidator).validateCreateRequest(request, schema);
        order.verify(mdmsDataEnricher).enrichCreateRequest(request, schema);
        order.verify(mdmsDataRepository).create(request);
    }

    @Test
    void create_nullSchema_returnsMdms() {
        Mdms mdms = mock(Mdms.class);
        MdmsRequest request = mock(MdmsRequest.class);
        when(request.getMdms()).thenReturn(mdms);
        when(schemaUtil.getSchema(request)).thenReturn(null);

        assertThat(mdmsService.create(request)).containsExactly(mdms);
        verify(mdmsDataValidator).validateCreateRequest(request, null);
    }

    @Test
    void create_nullRequest_throwsNullPointerException() {
        assertThatThrownBy(() -> mdmsService.create(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_nullMdms_returnsSingletonNull() {
        MdmsRequest request = mock(MdmsRequest.class);
        when(request.getMdms()).thenReturn(null);
        when(schemaUtil.getSchema(request)).thenReturn(new JSONObject());

        List<Mdms> created = mdmsService.create(request);

        assertThat(created).hasSize(1);
        assertThat(created.get(0)).isNull();
    }

    @Test
    void create_validatorThrows_doesNotPersist() {
        MdmsRequest request = mock(MdmsRequest.class);
        JSONObject schema = new JSONObject();
        when(schemaUtil.getSchema(request)).thenReturn(schema);
        doThrow(new CustomException("DUPLICATE_RECORD", "Duplicate record"))
                .when(mdmsDataValidator).validateCreateRequest(request, schema);

        assertThatThrownBy(() -> mdmsService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Duplicate record");
        verify(mdmsDataEnricher, never()).enrichCreateRequest(any(), any());
        verify(mdmsDataRepository, never()).create(any());
    }

    @Test
    void create_enricherThrows_doesNotPersist() {
        MdmsRequest request = mock(MdmsRequest.class);
        JSONObject schema = new JSONObject();
        when(schemaUtil.getSchema(request)).thenReturn(schema);
        doThrow(new CustomException("ENRICH_ERR", "enrich failed"))
                .when(mdmsDataEnricher).enrichCreateRequest(request, schema);

        assertThatThrownBy(() -> mdmsService.create(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("enrich failed");
        verify(mdmsDataRepository, never()).create(any());
    }

    @Test
    void search_singleMaster_returnsModuleMasterMap() {
        SearchInput input = requestFor("pg.citya", Collections.singletonList(module("common-masters", "Department", null)));
        when(multiStateInstanceUtil.getStateLevelTenant("pg.citya")).thenReturn("pg");
        when(mdmsDataRepository.search(any())).thenReturn(
                tenants(rows("DEPT"), "common-masters.Department", "pg.citya", "pg"));

        Map<String, Map<String, JSONArray>> result = mdmsService.search(input.request);

        assertThat(result).containsKey("common-masters");
        assertThat(result.get("common-masters")).containsKey("Department");
        assertThat(result.get("common-masters").get("Department")).hasSize(1);
        verify(input.criteria).setTenantId("pg");
    }

    @Test
    void search_twoMasters_groupsByModule() {
        MasterDetail department = master("Department", null);
        MasterDetail designation = master("Designation", null);
        ModuleDetail module = mock(ModuleDetail.class);
        when(module.getModuleName()).thenReturn("common-masters");
        when(module.getMasterDetails()).thenReturn(Arrays.asList(department, designation));

        SearchInput input = requestFor("pg", Collections.singletonList(module));
        when(multiStateInstanceUtil.getStateLevelTenant("pg")).thenReturn("pg");

        Map<String, JSONArray> schemas = new HashMap<String, JSONArray>();
        schemas.put("common-masters.Department", rows("DEPT"));
        schemas.put("common-masters.Designation", rows("DESIG"));
        Map<String, Map<String, JSONArray>> repoResult = new HashMap<String, Map<String, JSONArray>>();
        repoResult.put("pg", schemas);
        when(mdmsDataRepository.search(any())).thenReturn(repoResult);

        Map<String, Map<String, JSONArray>> result = mdmsService.search(input.request);

        assertThat(result.get("common-masters")).containsKeys("Department", "Designation");
    }

    @Test
    void search_emptyModuleDetails_returnsEmptyMap() {
        SearchInput input = requestFor("pg", Collections.<ModuleDetail>emptyList());
        when(multiStateInstanceUtil.getStateLevelTenant("pg")).thenReturn("pg");
        when(mdmsDataRepository.search(any())).thenReturn(new HashMap<String, Map<String, JSONArray>>());

        assertThat(mdmsService.search(input.request)).isEmpty();
    }

    @Test
    void search_nullRequest_throwsNullPointerException() {
        assertThatThrownBy(() -> mdmsService.search(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void search_nullCriteria_throwsNullPointerException() {
        MdmsCriteriaReq request = mock(MdmsCriteriaReq.class);
        when(request.getMdmsCriteria()).thenReturn(null);

        assertThatThrownBy(() -> mdmsService.search(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void search_nullTenantId_throwsNullPointerException() {
        SearchInput input = requestFor(null, Collections.<ModuleDetail>emptyList());

        assertThatThrownBy(() -> mdmsService.search(input.request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void search_nullModuleDetails_throwsNullPointerException() {
        SearchInput input = requestFor("pg", null);
        when(multiStateInstanceUtil.getStateLevelTenant("pg")).thenReturn("pg");

        assertThatThrownBy(() -> mdmsService.search(input.request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void search_repositoryReturnsNull_throwsNullPointerException() {
        SearchInput input = requestFor("pg", Collections.singletonList(module("common-masters", "Department", null)));
        when(multiStateInstanceUtil.getStateLevelTenant("pg")).thenReturn("pg");
        when(mdmsDataRepository.search(any())).thenReturn(null);

        assertThatThrownBy(() -> mdmsService.search(input.request)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void search_blankFilter_keepsAllMasters(String filter) {
        SearchInput input = requestFor("pg", Collections.singletonList(module("common-masters", "Department", filter)));
        when(multiStateInstanceUtil.getStateLevelTenant("pg")).thenReturn("pg");
        when(mdmsDataRepository.search(any())).thenReturn(tenants(rows("A", "B"), "common-masters.Department", "pg"));

        Map<String, Map<String, JSONArray>> result = mdmsService.search(input.request);

        assertThat(result.get("common-masters").get("Department")).hasSize(2);
    }

    @Test
    void search_matchingFilter_returnsSubset() {
        SearchInput input = requestFor("pg", Collections.singletonList(
                module("common-masters", "Department", "$[?(@.code == 'A')]")));
        when(multiStateInstanceUtil.getStateLevelTenant("pg")).thenReturn("pg");
        when(mdmsDataRepository.search(any())).thenReturn(tenants(rows("A", "B"), "common-masters.Department", "pg"));

        Map<String, Map<String, JSONArray>> result = mdmsService.search(input.request);

        JSONArray department = result.get("common-masters").get("Department");
        assertThat(department).hasSize(1);
        assertThat(String.valueOf(department.get(0))).contains("A");
    }

    @Test
    void search_invalidFilter_throwsRuntimeException() {
        SearchInput input = requestFor("pg", Collections.singletonList(module("common-masters", "Department", "$[")));
        when(multiStateInstanceUtil.getStateLevelTenant("pg")).thenReturn("pg");
        when(mdmsDataRepository.search(any())).thenReturn(tenants(rows("A"), "common-masters.Department", "pg"));

        assertThatThrownBy(() -> mdmsService.search(input.request)).isInstanceOf(RuntimeException.class);
    }

    private static ModuleDetail module(String moduleName, String masterName, String filter) {
        ModuleDetail module = mock(ModuleDetail.class);
        when(module.getModuleName()).thenReturn(moduleName);
        when(module.getMasterDetails()).thenReturn(Collections.singletonList(master(masterName, filter)));
        return module;
    }

    private static MasterDetail master(String name, String filter) {
        MasterDetail master = mock(MasterDetail.class);
        when(master.getName()).thenReturn(name);
        when(master.getFilter()).thenReturn(filter);
        return master;
    }

    private static JSONArray rows(String... codes) {
        JSONArray array = new JSONArray();
        for (String code : codes) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("code", code);
            array.add(row);
        }
        return array;
    }

    private static Map<String, Map<String, JSONArray>> tenants(JSONArray rows, String schemaCode, String... tenantIds) {
        Map<String, Map<String, JSONArray>> all = new HashMap<String, Map<String, JSONArray>>();
        for (String tenantId : tenantIds) {
            Map<String, JSONArray> schemas = new HashMap<String, JSONArray>();
            schemas.put(schemaCode, rows);
            all.put(tenantId, schemas);
        }
        return all;
    }

    private static SearchInput requestFor(String tenantId, List<ModuleDetail> modules) {
        MdmsCriteria criteria = mock(MdmsCriteria.class);
        AtomicReference<Map<String, String>> filters = new AtomicReference<Map<String, String>>();
        when(criteria.getTenantId()).thenReturn(tenantId);
        when(criteria.getModuleDetails()).thenReturn(modules);
        doAnswer(invocation -> {
            filters.set(invocation.getArgument(0));
            return null;
        }).when(criteria).setSchemaCodeFilterMap(any());
        when(criteria.getSchemaCodeFilterMap()).thenAnswer(invocation -> filters.get());

        MdmsCriteriaReq request = mock(MdmsCriteriaReq.class);
        when(request.getMdmsCriteria()).thenReturn(criteria);
        return new SearchInput(request, criteria);
    }

    private static final class SearchInput {
        private final MdmsCriteriaReq request;
        private final MdmsCriteria criteria;

        private SearchInput(MdmsCriteriaReq request, MdmsCriteria criteria) {
            this.request = request;
            this.criteria = criteria;
        }
    }
}
