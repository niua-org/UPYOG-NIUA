// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.calculator.config.CalculatorConfig;
import org.egov.asset.calculator.kafka.broker.Producer;
import org.egov.asset.calculator.repository.AssetRepository;
import org.egov.asset.calculator.repository.DepreciationDetailRepository;
import org.egov.asset.calculator.repository.MdmsDataRepository;
import org.egov.asset.calculator.services.EnrichmentService;
import org.egov.asset.calculator.utils.AssetCalculatorUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ProcessDepreciationV2Test {
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private DepreciationDetailRepository depreciationDetailRepository;
    @Mock
    private MdmsDataRepository mdmsDataRepository;
    @Mock
    private Producer producer;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private AssetCalculatorUtil assetCalculatorUtil;
    @Mock
    private CalculatorConfig config;

    private ProcessDepreciationV2 subject;

    @BeforeEach
    void setUp() {
        subject = new ProcessDepreciationV2(assetRepository, depreciationDetailRepository, mdmsDataRepository, producer, enrichmentService, assetCalculatorUtil, config);
    }


    @Test
    void calculateDepreciation_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
