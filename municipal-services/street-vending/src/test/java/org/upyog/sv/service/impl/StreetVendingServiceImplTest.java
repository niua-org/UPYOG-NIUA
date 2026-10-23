// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.sv.repository.StreetVendingRepository;
import org.upyog.sv.service.DemandService;
import org.upyog.sv.service.EnrichmentService;
import org.upyog.sv.service.MdmsCacheService;
import org.upyog.sv.service.StreetVendingEncryptionService;
import org.upyog.sv.service.WorkflowService;
import org.upyog.sv.util.MdmsUtil;
import org.upyog.sv.validator.StreetVendingValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StreetVendingServiceImplTest {
    @Mock
    private MdmsUtil util;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private StreetVendingRepository streetVendingRepository;
    @Mock
    private DemandService demandService;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private StreetVendingValidator validator;
    @Mock
    private StreetVendingEncryptionService encryptionService;
    @Mock
    private MdmsCacheService mdmsCache;

    private StreetVendingServiceImpl subject;

    @BeforeEach
    void setUp() {
        subject = new StreetVendingServiceImpl(util, enrichmentService, streetVendingRepository, demandService, workflowService, validator, encryptionService, mdmsCache);
    }


    @Test
    void createStreetVendingApplication_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
