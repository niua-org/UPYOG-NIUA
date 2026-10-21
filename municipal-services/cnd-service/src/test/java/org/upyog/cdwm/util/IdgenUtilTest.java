// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.cdwm.config.CNDConfiguration;
import org.upyog.cdwm.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IdgenUtilTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository restRepo;
    @Mock
    private CNDConfiguration config;

    private IdgenUtil subject;

    @BeforeEach
    void setUp() {
        subject = new IdgenUtil(mapper, restRepo, config);
    }


    @Test
    void getIdList_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
