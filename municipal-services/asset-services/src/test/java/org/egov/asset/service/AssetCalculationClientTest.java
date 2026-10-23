// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.asset.config.AssetConfiguration;
import org.egov.asset.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetCalculationClientTest {
    @Mock
    private ServiceRequestRepository apiClient;
    @Mock
    private AssetConfiguration config;
    @Mock
    private ObjectMapper objectMapper;

    private AssetCalculationClient subject;

    @BeforeEach
    void setUp() {
        subject = new AssetCalculationClient(apiClient, config, objectMapper);
    }


    @Test
    void triggerDepreciationCalculation_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
