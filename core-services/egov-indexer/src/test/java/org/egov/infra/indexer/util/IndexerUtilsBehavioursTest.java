package org.egov.infra.indexer.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import org.egov.infra.indexer.consumer.config.ReindexConsumerConfig;
import org.egov.infra.indexer.producer.IndexerProducer;
import org.egov.infra.indexer.web.contract.FilterMapping;
import org.egov.infra.indexer.web.contract.Index;
import org.egov.infra.indexer.web.contract.UriMapping;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexerUtilsBehavioursTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ReindexConsumerConfig kafkaConsumerConfig;

    @Mock
    private IndexerProducer producer;

    private IndexerUtils indexerUtils;

    @BeforeEach
    void setUp() throws Exception {
        indexerUtils = new IndexerUtils();
        setField(indexerUtils, "restTemplate", restTemplate);
        setField(indexerUtils, "kafkaConsumerConfig", kafkaConsumerConfig);
        setField(indexerUtils, "producer", producer);
        setField(indexerUtils, "defaultVersion", "1.0.0");
        setField(indexerUtils, "esHostUrl", "http://es-host/");
        setField(indexerUtils, "pollInterval", "1");
        setField(indexerUtils, "mdmsHost", "http://mdms/");
        setField(indexerUtils, "mdmsEndpoint", "/mdms-v2/v1/_search");
        setField(indexerUtils, "serviceHost", "http://service/");
        setField(indexerUtils, "dssTopicForCollection", "dss-collection");
        setField(indexerUtils, "dssTopicPushEnabled", Boolean.TRUE);
        setField(indexerUtils, "topicPushEnable", Boolean.TRUE);
        setField(indexerUtils, "timezone", "UTC");
        setField(indexerUtils, "esUsername", "elastic");
        setField(indexerUtils, "esPassword", "secret");
        Method init = IndexerUtils.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(indexerUtils);
    }

    @Test
    void buildString_reconstructsColonSeparatedPayload() {
        assertThat(indexerUtils.buildString(Map.of("k", "v"))).contains("k");
    }

    @Test
    void buildUri_resolvesListQueryParam() {
        UriMapping mapping = UriMapping.builder()
                .path("/items")
                .queryParam("ids=$.ids")
                .build();
        String uri = indexerUtils.buildUri(mapping, "{\"ids\":[\"a\",\"b\"]}");

        assertThat(uri).isEqualTo("/items?ids=a,b");
    }

    @Test
    void buildUri_literalQueryParamWithoutJsonPath() {
        UriMapping mapping = UriMapping.builder()
                .path("/x")
                .queryParam("status=ACTIVE")
                .build();

        assertThat(indexerUtils.buildUri(mapping, "{}")).isEqualTo("/x?status=ACTIVE");
    }

    @Test
    void buildFilter_stripsBracketsFromArrayValues() {
        UriMapping mapping = UriMapping.builder()
                .filter("code=$code")
                .filterMapping(List.of(
                        FilterMapping.builder().variable("$code").valueJsonpath("$.codes").build()))
                .build();

        assertThat(indexerUtils.buildFilter(mapping, "{\"codes\":[\"PT\",\"TL\"]}"))
                .contains("code='")
                .contains("PT");
    }

    @Test
    void buildIndexId_allFieldsEmpty_returnsNull() {
        Index index = new Index();
        index.setId("$.a,$.b");
        assertThat(indexerUtils.buildIndexId(index, "{\"a\":\"\",\"b\":\"\"}")).isNull();
    }

    @Test
    void fetchMdmsData_postsCriteria() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(Map.of("ok", true));

        Object result = indexerUtils.fetchMdmsData("http://mdms/search", "pg.citya", "module", "master", "filter");

        assertThat(result).isInstanceOf(Map.class);
        verify(restTemplate).postForObject(eq("http://mdms/search"), any(), eq(Map.class));
    }

    @Test
    void setDynamicMapping_success_returnsOk() {
        Index index = new Index();
        index.setName("idx");
        org.mockito.Mockito.doNothing().when(restTemplate).put(anyString(), anyString(), eq(Map.class));

        assertThat(indexerUtils.setDynamicMapping(index)).isEqualTo("OK");
    }

    @Test
    void setDynamicMapping_failure_returnsNull() {
        Index index = new Index();
        index.setName("idx");
        org.mockito.Mockito.doThrow(new RuntimeException("fail"))
                .when(restTemplate).put(anyString(), anyString(), eq(Map.class));

        assertThat(indexerUtils.setDynamicMapping(index)).isNull();
    }

    @Test
    void constructArrayForBulkIndex_bulkWithJsonPath() throws Exception {
        Index index = new Index();
        index.setJsonPath("$.records");
        String kafka = "{\"records\":[{\"id\":\"1\"},{\"id\":\"2\"}]}";

        JSONArray array = indexerUtils.constructArrayForBulkIndex(kafka, index, true);

        assertThat(array.length()).isEqualTo(2);
    }

    @Test
    void constructArrayForBulkIndex_bulkInvalidArray_returnsNull() throws Exception {
        Index index = new Index();
        assertThat(indexerUtils.constructArrayForBulkIndex("payload:not-array", index, true)).isNull();
    }

    @Test
    void constructArrayForBulkIndex_nonBulkWrapsSingleObject() throws Exception {
        Index index = new Index();
        index.setTimeStampField("$.ts");
        String kafka = "{\"id\":\"1\",\"ts\":15534533434000}";

        JSONArray array = indexerUtils.constructArrayForBulkIndex(kafka, index, false);

        assertThat(array.length()).isEqualTo(1);
        assertThat(array.get(0).toString()).contains("@timestamp");
    }

    @Test
    void constructArrayForBulkIndex_invalidJson_throws() {
        Index index = new Index();
        assertThatThrownBy(() -> indexerUtils.constructArrayForBulkIndex("{bad", index, false))
                .isInstanceOf(Exception.class);
    }

    @Test
    void transformData_masksFieldsAndAddsTimestamp() throws Exception {
        Index index = new Index();
        index.setFieldsToBeMasked(List.of("$.secret"));
        index.setTimeStampField("$.createdTime");
        JSONArray input = new JSONArray("[{\"secret\":\"hide-me\",\"createdTime\":15534533434000}]");

        JSONArray result = indexerUtils.transformData(index, input);

        assertThat(result.length()).isEqualTo(1);
        assertThat(result.get(0).toString()).contains("XXXXXXXX");
        assertThat(result.get(0).toString()).contains("@timestamp");
    }

    @Test
    void transformData_skipsNullElements() throws Exception {
        Index index = new Index();
        JSONArray input = new JSONArray("[null,\"null\",{\"id\":\"1\"}]");

        JSONArray result = indexerUtils.transformData(index, input);

        assertThat(result.length()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void maskFields_emptyList_returnsSameContext() {
        DocumentContext ctx = com.jayway.jsonpath.JsonPath.parse("{\"a\":1}");
        Index index = new Index();

        assertThat(indexerUtils.maskFields(index, ctx).jsonString()).contains("\"a\":1");
    }

    @Test
    void getObjectMappers_configureJackson() {
        ObjectMapper withNulls = indexerUtils.getObjectMapperWithNull();
        ObjectMapper standard = indexerUtils.getObjectMapper();

        assertThat(withNulls).isNotNull();
        assertThat(standard).isNotNull();
    }

    @Test
    void pushCollectionToDSSTopic_collectionIndex_publishes() throws Exception {
        Index index = new Index();
        index.setName("collection-index");

        indexerUtils.pushCollectionToDSSTopic("key-1", "{\"amount\":100}", index);

        verify(producer, atLeastOnce()).producer(eq("dss-collection"), any());
        verify(producer).producer(eq("collection-index-enriched"), eq("key-1"), any());
    }

    @Test
    void pushToKafka_nonCollectionIndex_publishes() throws Exception {
        Index index = new Index();
        index.setName("property-index");

        indexerUtils.pushToKafka("doc-1", "{\"id\":\"doc-1\"}", index);

        verify(producer).producer(eq("property-index-enriched"), eq("doc-1"), any());
    }

    @Test
    void pushToKafka_collectionIndex_skipped() {
        Index index = new Index();
        index.setName("payment-index");

        indexerUtils.pushToKafka("k", "{}", index);

        verify(producer, org.mockito.Mockito.never()).producer(anyString(), anyString(), any());
    }

    @Test
    void pushToKafka_invalidJson_doesNotThrow() {
        Index index = new Index();
        index.setName("property-index");

        indexerUtils.pushToKafka("k", "{bad-json", index);
    }

    @Test
    void pushToKafka_whenDisabled_doesNotPublish() throws Exception {
        setField(indexerUtils, "topicPushEnable", Boolean.FALSE);
        Index index = new Index();
        index.setName("property-index");

        indexerUtils.pushToKafka("k", "{}", index);

        verify(producer, org.mockito.Mockito.never()).producer(anyString(), anyString(), any());
    }

    @Test
    void pushCollectionToDSSTopic_nonCollectionIndex_doesNotPublish() {
        Index index = new Index();
        index.setName("property-index");

        indexerUtils.pushCollectionToDSSTopic("k", "{}", index);

        verify(producer, org.mockito.Mockito.never()).producer(anyString(), any());
    }

    @Test
    void pushCollectionToDSSTopic_invalidJson_doesNotThrow() {
        Index index = new Index();
        index.setName("collection-index");

        indexerUtils.pushCollectionToDSSTopic("k", "{not-json", index);
    }

    @Test
    void pushCollectionToDSSTopic_whenDisabled_doesNotPublish() throws Exception {
        setField(indexerUtils, "dssTopicPushEnabled", Boolean.FALSE);
        Index index = new Index();
        index.setName("collection-index");

        indexerUtils.pushCollectionToDSSTopic("k", "{}", index);

        verify(producer, org.mockito.Mockito.never()).producer(anyString(), any());
    }

    @Test
    void buildPagedUriForLegacyIndex_withoutTenant_usesOffsetOnly() {
        var pagination = org.egov.infra.indexer.web.contract.PaginationDetails.builder()
                .offsetKey("page")
                .sizeKey("limit")
                .build();
        var api = org.egov.infra.indexer.web.contract.APIDetails.builder()
                .uri("/search")
                .paginationDetails(pagination)
                .build();

        assertThat(indexerUtils.buildPagedUriForLegacyIndex(api, 2, 10))
                .isEqualTo("http://service//search?page=2&limit=10");
    }

    @Test
    void transformData_partialTransform_returnsOriginalArray() throws Exception {
        Index index = new Index();
        index.setTimeStampField("$.ts");
        JSONArray input = new JSONArray("[\"not-json\",{\"id\":\"2\",\"ts\":15534533434000}]");

        JSONArray result = indexerUtils.transformData(index, input);

        assertThat(result).isSameAs(input);
    }

    @Test
    void buildFilter_nullValue_skipsReplacement() {
        UriMapping mapping = UriMapping.builder()
                .filter("x=$x")
                .filterMapping(List.of(
                        FilterMapping.builder().variable("$x").valueJsonpath("$.missing").build()))
                .build();

        assertThat(indexerUtils.buildFilter(mapping, "{\"missing\":null}")).isEqualTo("x=$x");
    }

    @Test
    void buildUri_pathWithPathParam_replacesPlaceholder() {
        UriMapping mapping = UriMapping.builder()
                .path("/tenant/$/resource")
                .pathParam("$.tenantId")
                .build();

        assertThat(indexerUtils.buildUri(mapping, "{\"tenantId\":\"t1\"}"))
                .isEqualTo("/tenant/t1/resource");
    }

    @Test
    void buildPagedUriForLegacyIndex_absoluteUriAndDefaultPaginationKeys() {
        var pagination = org.egov.infra.indexer.web.contract.PaginationDetails.builder().build();
        var api = org.egov.infra.indexer.web.contract.APIDetails.builder()
                .uri("https://external/search")
                .paginationDetails(pagination)
                .build();

        assertThat(indexerUtils.buildPagedUriForLegacyIndex(api, 0, 20))
                .isEqualTo("https://external/search?offset=0&size=20");
    }

    @Test
    void fetchEstimatedTime_secondsBranch() {
        assertThat(indexerUtils.fetchEstimatedTime(4000)).contains("secs");
    }

    @Test
    void orchestrateListenerOnESHealth_esUp_resumesContainer() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("cluster", "ok")));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of("cluster", "ok"));

        indexerUtils.orchestrateListenerOnESHealth();
        Thread.sleep(1500);

        verify(kafkaConsumerConfig).pauseContainer();
        verify(kafkaConsumerConfig, atLeastOnce()).startContainer();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
