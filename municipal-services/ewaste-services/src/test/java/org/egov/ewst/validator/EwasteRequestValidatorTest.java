// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.ewst.repository.EwasteApplicationRepository;
import org.egov.ewst.util.EwasteUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteRequestValidatorTest {
    @Mock
    private EwasteApplicationRepository repository;
    @Mock
    private EwasteUtil ewasteUtil;

    private EwasteRequestValidator subject;

    @BeforeEach
    void setUp() {
        subject = new EwasteRequestValidator(repository, ewasteUtil);
    }


    @Test
    void validateCreateRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
