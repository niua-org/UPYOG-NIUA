package org.upyog.reconciliation.extractor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExtractorFactoryTest {

    @Mock
    private Map<String, MetricDataExtractor> extractorMap;

    @InjectMocks
    private DataExtractorFactory factory;

    @Test
    void getExtractor_knownType_returnsBean() {
        MetricDataExtractor extractor = (tenantId, module, date) -> null;
        when(extractorMap.get("ELASTICSEARCH")).thenReturn(extractor);

        assertThat(factory.getExtractor("ELASTICSEARCH")).isSameAs(extractor);
    }

    @Test
    void getExtractor_unknownType_throwsIllegalArgumentException() {
        when(extractorMap.get("UNKNOWN")).thenReturn(null);

        assertThatThrownBy(() -> factory.getExtractor("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown data source type");
    }
}
