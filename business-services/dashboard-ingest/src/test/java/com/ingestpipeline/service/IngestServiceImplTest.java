package com.ingestpipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingestpipeline.model.ErrorWrapper;
import com.ingestpipeline.model.TopicContext;
import com.ingestpipeline.model.TopicContextConfig;
import com.ingestpipeline.producer.IngestProducer;
import com.ingestpipeline.util.ApplicationProperties;
import com.ingestpipeline.util.ConfigLoader;
import com.ingestpipeline.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestServiceImplTest {

    @Mock
    private IngestProducer ingestProducer;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ConfigLoader configLoader;
    @Mock
    private DigressService digressService;

    @InjectMocks
    private IngestServiceImpl ingestService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ingestService, "mapper", new ObjectMapper());
    }

    @Test
    void ingestToPipeline_validateRule_pushesToIngestTopic() {
        Map<String, Boolean> rules = Map.of(Constants.PipelineRules.VALIDATE_DATA, true);
        when(applicationProperties.getPipelineRules()).thenReturn(rules);
        when(applicationProperties.getTransactionIngestTopic()).thenReturn("ingest-topic");
        when(applicationProperties.getTransactionIngestKey()).thenReturn("ingest-key");

        assertThat(ingestService.ingestToPipeline(Map.of("field", "value"))).isTrue();

        verify(ingestProducer).pushToPipeline(eq(Map.of("field", "value")), eq("ingest-topic"), isNull());
    }

    @Test
    void ingestToPipeline_transformRule_pushesToValidationTopic() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put(Constants.PipelineRules.VALIDATE_DATA, false);
        rules.put(Constants.PipelineRules.TRANSFORM_DATA, true);
        when(applicationProperties.getPipelineRules()).thenReturn(rules);
        when(applicationProperties.getTransactionValidationTopic()).thenReturn("validate-topic");
        when(applicationProperties.getTransactionValidationKey()).thenReturn("validate-key");

        ingestService.ingestToPipeline("payload");

        verify(ingestProducer).pushToPipeline(eq("payload"), eq("validate-topic"), isNull());
    }

    @Test
    void ingestToPipeline_enrichRule_pushesToTransformationTopic() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put(Constants.PipelineRules.VALIDATE_DATA, false);
        rules.put(Constants.PipelineRules.TRANSFORM_DATA, false);
        rules.put(Constants.PipelineRules.ENRICH_DATA, true);
        when(applicationProperties.getPipelineRules()).thenReturn(rules);
        when(applicationProperties.getTransactionTransformationTopic()).thenReturn("transform-topic");
        when(applicationProperties.getTransactionTransformationKey()).thenReturn("transform-key");

        ingestService.ingestToPipeline(42);

        verify(ingestProducer).pushToPipeline(eq(42), eq("transform-topic"), isNull());
    }

    @Test
    void ingestToPipeline_ruleLookupFailure_pushesErrorIntent() {
        when(applicationProperties.getPipelineRules()).thenThrow(new RuntimeException("rules unavailable"));

        ingestService.ingestToPipeline(Map.of("broken", true));

        verify(ingestProducer).pushToPipeline(any(ErrorWrapper.class), eq(Constants.KafkaTopics.ERROR_INTENT), isNull());
        verify(ingestProducer).pushToPipeline(eq(Map.of("broken", true)), eq(""), isNull());
    }

    @Test
    void loadTopicsConfig_populatesTopicContextMap() throws Exception {
        TopicContext topicContext = new TopicContext();
        topicContext.setTopic("dss-collection-update");
        topicContext.setDataContext("collection");
        topicContext.setDataContextVersion("v1");
        TopicContextConfig config = new TopicContextConfig();
        config.setTopicContexts(List.of(topicContext));
        when(configLoader.get(IngestService.TOPIC_CONTEXT_CONFIG)).thenReturn(new ObjectMapper().writeValueAsString(config));

        ingestService.loadTopicsConfig();

        assertThat(ingestService.getContextForIncomingTopic("dss-collection-update").getDataContext())
                .isEqualTo("collection");
    }

    @Test
    void loadTopicsConfig_invalidJson_leavesContextEmpty() {
        when(configLoader.get(IngestService.TOPIC_CONTEXT_CONFIG)).thenReturn("not-json");

        ingestService.loadTopicsConfig();

        assertThat(ingestService.getContextForIncomingTopic("missing")).isNull();
    }

    @Test
    void getContextForIncomingTopic_returnsContextWhenLoaded() {
        TopicContext topicContext = new TopicContext();
        topicContext.setTopic("ingest");
        topicContext.setDataContext("collection");
        topicContext.setDataContextVersion("v1");
        Map<String, TopicContext> map = new HashMap<>();
        map.put("ingest", topicContext);
        ReflectionTestUtils.setField(ingestService, "topicContextMap", map);

        var incoming = ingestService.getContextForIncomingTopic("ingest");

        assertThat(incoming).isNotNull();
        assertThat(incoming.getDataContext()).isEqualTo("collection");
        assertThat(incoming.getDataContextVersion()).isEqualTo("v1");
    }

    @Test
    void getContextForIncomingTopic_emptyTopicMap_returnsNull() {
        ReflectionTestUtils.setField(ingestService, "topicContextMap", new HashMap<>());

        assertThat(ingestService.getContextForIncomingTopic("unknown")).isNull();
    }
}
