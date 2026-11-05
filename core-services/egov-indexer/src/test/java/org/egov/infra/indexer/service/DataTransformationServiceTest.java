package org.egov.infra.indexer.service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.egov.infra.indexer.util.IndexerUtils;
import org.egov.infra.indexer.web.contract.CustomJsonMapping;
import org.egov.infra.indexer.web.contract.FieldMapping;
import org.egov.infra.indexer.web.contract.FilterMapping;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.infra.indexer.web.contract.UriMapping;
import org.egov.tracer.model.CustomException;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataTransformationServiceTest {

    @Mock
    private IndexerUtils indexerUtils;

    @Mock
    private RestTemplate restTemplate;

    private DataTransformationService service;

    @BeforeEach
    void setUp() {
        service = new DataTransformationService();
        setField(service, "indexerUtils", indexerUtils);
        setField(service, "restTemplate", restTemplate);
        setField(service, "mdmsHost", "http://mdms/");
        setField(service, "mdmsEndpoint", "/mdms-v2/v1/_search");
    }

    @Test
    void appendIdToJson_emptyId_returnsNull() {
        Index index = new Index();
        when(indexerUtils.buildIndexId(any(), anyString())).thenReturn("");

        assertThat(service.appendIdToJson(index, new StringBuilder(), "{}", null)).isNull();
    }

    @Test
    void appendIdToJson_validId_appendsBulkHeaderAndPayload() {
        Index index = new Index();
        when(indexerUtils.buildIndexId(any(), anyString())).thenReturn("doc-1");
        StringBuilder builder = new StringBuilder();

        StringBuilder result = service.appendIdToJson(index, builder, "{\"id\":\"1\"}", null);

        assertThat(result).isSameAs(builder);
        assertThat(result.toString()).contains("doc-1");
        assertThat(result.toString()).contains("{\"id\":\"1\"}");
    }

    @Test
    void buildCustomJsonForIndex_missingIndexMapping_throwsMissingCustomMapping() {
        CustomJsonMapping mapping = new CustomJsonMapping();

        assertThatThrownBy(() -> service.buildCustomJsonForIndex(mapping, "{}"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("code", "EGOV_INDEXER_MISSING_CUSTOM_MAPPING");
    }

    @Test
    void buildJsonForIndex_nonCustomBulk_includesBulkHeaderAndDocument() throws Exception {
        Index index = new Index();
        index.setId("$.id");
        JSONArray array = new JSONArray("[{\"id\":\"doc-9\"}]");
        when(indexerUtils.constructArrayForBulkIndex(anyString(), any(), anyBoolean())).thenReturn(array);
        when(indexerUtils.buildString(any())).thenReturn("{\"id\":\"doc-9\"}");
        when(indexerUtils.buildIndexId(any(), anyString())).thenReturn("doc-9");

        String bulkJson = service.buildJsonForIndex(index, "{\"id\":\"doc-9\"}", false, false);

        assertThat(bulkJson).contains("doc-9");
        verify(indexerUtils).pushToKafka(eq("doc-9"), eq("{\"id\":\"doc-9\"}"), eq(index));
    }

    @Test
    void buildJsonForIndex_customBulk_buildsFromCustomMapping() throws Exception {
        Index index = new Index();
        index.setId("$.id");
        CustomJsonMapping custom = new CustomJsonMapping();
        custom.setIndexMapping(Map.of("tenantId", "placeholder"));
        custom.setFieldMapping(List.of(
                FieldMapping.builder().injsonpath("$.tenantId").outJsonPath("tenantId").build()));
        index.setCustomJsonMapping(custom);
        JSONArray array = new JSONArray("[{\"id\":\"c1\",\"tenantId\":\"pg.citya\"}]");
        when(indexerUtils.constructArrayForBulkIndex(anyString(), any(), anyBoolean())).thenReturn(array);
        when(indexerUtils.buildString(any())).thenReturn("{\"id\":\"c1\",\"tenantId\":\"pg.citya\"}");
        when(indexerUtils.buildIndexId(any(), anyString())).thenReturn("c1");
        when(indexerUtils.getProcessedJsonPath("tenantId")).thenReturn("tenantId");

        String bulk = service.buildJsonForIndex(index, "{}", false, true);

        assertThat(bulk).contains("c1");
        verify(indexerUtils).pushCollectionToDSSTopic(eq("c1"), anyString(), eq(index));
    }

    @Test
    void buildJsonForIndex_constructFailure_returnsNull() throws Exception {
        Index index = new Index();
        when(indexerUtils.constructArrayForBulkIndex(anyString(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("bad json"));

        assertThat(service.buildJsonForIndex(index, "{", false, false)).isNull();
    }

    @Test
    void enrichDataUsingExternalServices_mapsResponseFields() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping uriMapping = UriMapping.builder()
                .path("/external")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.name").outJsonPath("metadata.ownerName").build()))
                .build();
        mapping.setExternalUriMapping(List.of(uriMapping));
        DocumentContext ctx = JsonPath.parse(mutableMap("metadata", mutableMap("ownerName", "placeholder")));
        when(indexerUtils.buildUri(uriMapping, "{\"id\":\"1\"}")).thenReturn("http://svc/external");
        when(indexerUtils.getProcessedJsonPath("metadata.ownerName")).thenReturn("metadata");
        when(restTemplate.postForObject(eq("http://svc/external"), isNull(), eq(Map.class)))
                .thenReturn(Map.of("name", "Alice"));

        DocumentContext enriched = service.enrichDataUsingExternalServices(ctx, mapping, "{\"id\":\"1\"}");

        assertThat(enriched.read("$.metadata.ownerName", String.class)).isEqualTo("Alice");
    }

    @Test
    void enrichDataUsingExternalServices_withFilterOnFieldMapping() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        FieldMapping responseMapping = FieldMapping.builder()
                .injsonpath("$.code")
                .outJsonPath("payload.code")
                .build();
        UriMapping uriMapping = UriMapping.builder()
                .path("/md")
                .uriResponseMapping(List.of(responseMapping))
                .build();
        mapping.setExternalUriMapping(List.of(uriMapping));
        DocumentContext ctx = JsonPath.parse(mutableMap("payload", mutableMap("code", "x")));
        when(indexerUtils.buildUri(any(), anyString())).thenReturn("http://svc/md");
        when(indexerUtils.getProcessedJsonPath("payload.code")).thenReturn("payload");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(Map.of("code", "PT"));

        DocumentContext enriched = service.enrichDataUsingExternalServices(ctx, mapping, "{\"tenantId\":\"pg.citya\"}");

        assertThat(enriched.read("$.payload.code", String.class)).isEqualTo("PT");
    }

    @Test
    void enrichDataUsingExternalServices_restFailure_keepsContext() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping uriMapping = UriMapping.builder()
                .path("/fail")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.x").outJsonPath("y").build()))
                .build();
        mapping.setExternalUriMapping(List.of(uriMapping));
        DocumentContext ctx = JsonPath.parse("{\"y\":\"orig\"}");
        when(indexerUtils.buildUri(any(), anyString())).thenReturn("http://svc/fail");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenThrow(new RuntimeException("down"));

        DocumentContext enriched = service.enrichDataUsingExternalServices(ctx, mapping, "{}");

        assertThat(enriched.read("$.y", String.class)).isEqualTo("orig");
    }

    @Test
    void denormalizeDataFromMDMS_resolvesMasterValue() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping mdms = UriMapping.builder()
                .path("")
                .tenantId("pg.citya")
                .moduleName("tenant")
                .masterName("citymodule")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.MdmsRes.tenant.citymodule[0].code")
                                .outJsonPath("mdms.moduleCode").build()))
                .build();
        mapping.setMdmsMapping(List.of(mdms));
        DocumentContext ctx = JsonPath.parse(mutableMap("mdms", mutableMap("moduleCode", "placeholder")));
        when(indexerUtils.buildFilter(any(), anyString())).thenReturn("tenantId='pg.citya'");
        when(indexerUtils.getProcessedJsonPath("mdms.moduleCode")).thenReturn("mdms");
        when(indexerUtils.fetchMdmsData(anyString(), eq("pg.citya"), eq("tenant"), eq("citymodule"), anyString()))
                .thenReturn(Map.of("MdmsRes", Map.of("tenant", Map.of("citymodule", List.of(Map.of("code", "PT"))))));

        DocumentContext enriched = service.denormalizeDataFromMDMS(ctx, mapping, "{\"tenantId\":\"pg.citya\"}");

        assertThat(enriched.read("$.mdms.moduleCode", String.class)).isEqualTo("PT");
    }

    @Test
    void denormalizeDataFromMDMS_singleElementList_unwrapsValue() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping mdms = UriMapping.builder()
                .path("http://mdms/search")
                .tenantId("t1")
                .moduleName("m")
                .masterName("master")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.MdmsRes.m.master").outJsonPath("mdms.val").build()))
                .build();
        mapping.setMdmsMapping(List.of(mdms));
        DocumentContext ctx = JsonPath.parse(mutableMap("mdms", mutableMap("val", "x")));
        when(indexerUtils.buildFilter(any(), anyString())).thenReturn("f");
        when(indexerUtils.getProcessedJsonPath("mdms.val")).thenReturn("mdms");
        when(indexerUtils.fetchMdmsData(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("MdmsRes", Map.of("m", Map.of("master", List.of("ONLY")))));

        DocumentContext enriched = service.denormalizeDataFromMDMS(ctx, mapping, "{}");

        assertThat(enriched.read("$.mdms.val", String.class)).isEqualTo("ONLY");
    }

    @Test
    void enrichDataUsingExternalServices_nullResponse_skipsMapping() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping uriMapping = UriMapping.builder()
                .path("/external")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.x").outJsonPath("payload.x").build()))
                .build();
        mapping.setExternalUriMapping(List.of(uriMapping));
        DocumentContext ctx = JsonPath.parse(mutableMap("payload", mutableMap("x", "keep")));
        when(indexerUtils.buildUri(any(), anyString())).thenReturn("http://svc/external");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(null);

        DocumentContext enriched = service.enrichDataUsingExternalServices(ctx, mapping, "{}");

        assertThat(enriched.read("$.payload.x", String.class)).isEqualTo("keep");
    }

    @Test
    void denormalizeDataFromMDMS_nullResponse_keepsContext() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping mdms = UriMapping.builder()
                .path("http://mdms/search")
                .tenantId("t1")
                .moduleName("m")
                .masterName("master")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.x").outJsonPath("mdms.x").build()))
                .build();
        mapping.setMdmsMapping(List.of(mdms));
        DocumentContext ctx = JsonPath.parse(mutableMap("mdms", mutableMap("x", "keep")));
        when(indexerUtils.buildFilter(any(), anyString())).thenReturn("f");
        when(indexerUtils.fetchMdmsData(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);

        DocumentContext enriched = service.denormalizeDataFromMDMS(ctx, mapping, "{}");

        assertThat(enriched.read("$.mdms.x", String.class)).isEqualTo("keep");
    }

    @Test
    void denormalizeDataFromMDMS_fetchFailure_keepsContext() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping mdms = UriMapping.builder()
                .path("http://mdms/search")
                .tenantId("t1")
                .moduleName("m")
                .masterName("master")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.x").outJsonPath("mdms.x").build()))
                .build();
        mapping.setMdmsMapping(List.of(mdms));
        DocumentContext ctx = JsonPath.parse(mutableMap("mdms", mutableMap("x", "keep")));
        when(indexerUtils.buildFilter(any(), anyString())).thenReturn("f");
        when(indexerUtils.fetchMdmsData(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("mdms down"));

        DocumentContext enriched = service.denormalizeDataFromMDMS(ctx, mapping, "{}");

        assertThat(enriched.read("$.mdms.x", String.class)).isEqualTo("keep");
    }

    @Test
    void buildJsonForIndex_nullArrayElement_skipsEntry() throws Exception {
        Index index = new Index();
        index.setId("$.id");
        JSONArray array = new JSONArray("[null]");
        when(indexerUtils.constructArrayForBulkIndex(anyString(), any(), anyBoolean())).thenReturn(array);

        assertThat(service.buildJsonForIndex(index, "{}", false, false)).isEmpty();
    }

    @Test
    void denormalizeDataFromMDMS_multiElementList_keepsListValue() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping mdms = UriMapping.builder()
                .path("http://mdms/search")
                .tenantId("t1")
                .moduleName("m")
                .masterName("master")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.MdmsRes.m.master").outJsonPath("mdms.val").build()))
                .build();
        mapping.setMdmsMapping(List.of(mdms));
        DocumentContext ctx = JsonPath.parse(mutableMap("mdms", mutableMap("val", "x")));
        when(indexerUtils.buildFilter(any(), anyString())).thenReturn("f");
        when(indexerUtils.getProcessedJsonPath("mdms.val")).thenReturn("mdms");
        when(indexerUtils.fetchMdmsData(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("MdmsRes", Map.of("m", Map.of("master", List.of("A", "B")))));

        DocumentContext enriched = service.denormalizeDataFromMDMS(ctx, mapping, "{}");

        assertThat(enriched.read("$.mdms.val", List.class)).containsExactly("A", "B");
    }

    @Test
    void enrichDataUsingExternalServices_withFilterOnInputPath() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        FieldMapping responseMapping = FieldMapping.builder()
                .injsonpath("$.items[?(")
                .outJsonPath("payload.code")
                .filter("id=$id")
                .filterMapping(List.of(
                        FilterMapping.builder().variable("$id").valueJsonpath("$.id").build()))
                .build();
        UriMapping uriMapping = UriMapping.builder()
                .path("/external")
                .uriResponseMapping(List.of(responseMapping))
                .build();
        mapping.setExternalUriMapping(List.of(uriMapping));
        DocumentContext ctx = JsonPath.parse(mutableMap("payload", mutableMap("code", "x")));
        when(indexerUtils.buildUri(any(), anyString())).thenReturn("http://svc/external");
        when(indexerUtils.buildFilter(any(), anyString())).thenReturn("id='1')]");
        when(indexerUtils.getProcessedJsonPath("payload.code")).thenReturn("payload");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(Map.of("items", List.of(Map.of("code", "PT"))));

        DocumentContext enriched = service.enrichDataUsingExternalServices(ctx, mapping, "{\"id\":\"1\"}");

        assertThat(enriched.jsonString()).contains("payload");
    }

    @Test
    void enrichDataUsingExternalServices_missingJsonPath_putsNull() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        UriMapping uriMapping = UriMapping.builder()
                .path("/external")
                .uriResponseMapping(List.of(
                        FieldMapping.builder().injsonpath("$.missing").outJsonPath("payload.code").build()))
                .build();
        mapping.setExternalUriMapping(List.of(uriMapping));
        DocumentContext ctx = JsonPath.parse(mutableMap("payload", mutableMap("code", "orig")));
        when(indexerUtils.buildUri(any(), anyString())).thenReturn("http://svc/external");
        when(indexerUtils.getProcessedJsonPath("payload.code")).thenReturn("payload");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(Map.of("other", 1));

        DocumentContext enriched = service.enrichDataUsingExternalServices(ctx, mapping, "{}");

        assertThat(enriched.read("$.payload.code", Object.class)).isNull();
    }

    @Test
    void buildCustomJsonForIndex_fieldReadFailure_continues() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        mapping.setIndexMapping(mutableMap("a", mutableMap("b", "placeholder")));
        mapping.setFieldMapping(List.of(
                FieldMapping.builder().injsonpath("$.missing").outJsonPath("a.b").build(),
                FieldMapping.builder().injsonpath("$.ok").outJsonPath("a.c").build()));
        when(indexerUtils.getProcessedJsonPath("a.b")).thenReturn("a");
        when(indexerUtils.getProcessedJsonPath("a.c")).thenReturn("a");

        String json = service.buildCustomJsonForIndex(mapping, "{\"ok\":\"val\"}");

        assertThat(json).contains("val");
    }

    @Test
    void buildCustomJsonForIndex_withFieldMapping_returnsJsonDocument() {
        CustomJsonMapping mapping = new CustomJsonMapping();
        mapping.setIndexMapping(Map.of("tenantId", "placeholder"));
        FieldMapping fieldMapping = FieldMapping.builder()
                .injsonpath("$.tenantId")
                .outJsonPath("tenantId")
                .build();
        mapping.setFieldMapping(List.of(fieldMapping));
        when(indexerUtils.getProcessedJsonPath("tenantId")).thenReturn("tenantId");

        String json = service.buildCustomJsonForIndex(mapping, "{\"tenantId\":\"pg.citya\"}");

        assertThat(json).contains("tenantId");
    }

    private static Map<String, Object> mutableMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
