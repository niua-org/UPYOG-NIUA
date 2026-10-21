// generated-by: generate_per_class_unit_tests.py
package org.upyog.pgrai.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.pgrai.config.PGRConfiguration;
import org.upyog.pgrai.repository.PGRRepository;
import org.upyog.pgrai.util.HRMSUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ServiceRequestValidatorTest {
    @Mock
    private PGRConfiguration config;
    @Mock
    private PGRRepository repository;
    @Mock
    private HRMSUtil hrmsUtil;

    private ServiceRequestValidator subject;

    @BeforeEach
    void setUp() {
        subject = new ServiceRequestValidator(config, repository, hrmsUtil);
    }


    @Test
    void validateCreate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
