package com.ingestpipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingestpipeline.model.TopicContext;
import com.ingestpipeline.producer.IngestProducer;
import com.ingestpipeline.util.ApplicationProperties;
import com.ingestpipeline.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestServiceImplTest {

    @Mock
    private IngestProducer ingestProducer;
    @Mock
    private ApplicationProperties applicationProperties;
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
        Map<String, Boolean> rules = new HashMap<>();
        rules.put(Constants.PipelineRules.VALIDATE_DATA, true);
        when(applicationProperties.getPipelineRules()).thenReturn(rules);
        when(applicationProperties.getTransactionIngestTopic()).thenReturn("ingest-topic");
        when(applicationProperties.getTransactionIngestKey()).thenReturn("ingest-key");

        Boolean result = ingestService.ingestToPipeline(Map.of("field", "value"));

        assertThat(result).isTrue();
        verify(ingestProducer).pushToPipeline(eq(Map.of("field", "value")), eq("ingest-topic"), eq(null));
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
}
