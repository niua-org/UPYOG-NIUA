// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.tp.config.TreePruningConfiguration;
import org.upyog.tp.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserUtilTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private TreePruningConfiguration config;

    private UserUtil subject;

    @BeforeEach
    void setUp() {
        subject = new UserUtil(mapper, serviceRequestRepository, config);
    }


    @Test
    void userCall_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
