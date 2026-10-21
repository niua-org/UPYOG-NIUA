// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.tp.config.TreePruningConfiguration;
import org.upyog.tp.repository.IdGenRepository;
import org.upyog.tp.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private TreePruningConfiguration config;
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
    void enrichCreateTreePruningRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
