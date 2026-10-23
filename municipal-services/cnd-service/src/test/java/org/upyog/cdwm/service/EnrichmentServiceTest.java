// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.cdwm.config.CNDConfiguration;
import org.upyog.cdwm.repository.IdGenRepository;
import org.upyog.cdwm.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private CNDConfiguration config;
    @Mock
    private IdGenRepository idGenRepository;
    @Mock
    private UserService userService;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(config, idGenRepository, userService);
    }


    @Test
    void enrichCreateCNDRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
