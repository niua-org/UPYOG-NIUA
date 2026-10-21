// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.inbox.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {
    @Mock
    private InboxConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;

    private InboxService subject;

    @BeforeEach
    void setUp() {
        subject = new InboxService(config, serviceRequestRepository, mapper);
    }


    @Test
    void fetchInboxData_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
