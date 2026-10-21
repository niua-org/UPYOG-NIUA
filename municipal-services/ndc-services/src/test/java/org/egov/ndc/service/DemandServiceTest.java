// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.ndc.config.NDCConfiguration;
import org.egov.ndc.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {
    @Mock
    private NDCConfiguration ndcConfiguration;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository repository;

    private DemandService subject;

    @BeforeEach
    void setUp() {
        subject = new DemandService(ndcConfiguration, mapper, repository);
    }


    @Test
    void generateDemands_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
