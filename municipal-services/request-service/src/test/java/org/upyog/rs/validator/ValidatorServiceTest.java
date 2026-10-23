// generated-by: generate_per_class_unit_tests.py
package org.upyog.rs.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ValidatorServiceTest {
    @Mock
    private Validator validator;

    private ValidatorService subject;

    @BeforeEach
    void setUp() {
        subject = new ValidatorService(validator);
    }


    @Test
    void validateRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
