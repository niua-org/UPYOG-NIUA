// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.asset.repository.AssetRepository;
import org.egov.asset.service.EnrichmentService;
import org.egov.asset.service.WorkflowService;
import org.egov.asset.util.AssetValidator;
import org.egov.asset.util.MdmsUtil;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {
    @Mock
    private MdmsUtil util;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetValidator assetValidator;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private ObjectMapper objectMapper;

    private AssetService subject;

    @BeforeEach
    void setUp() {
        subject = new AssetService(util, assetRepository, assetValidator, enrichmentService, workflowService, modelMapper, objectMapper);
    }


    @Test
    void create_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
