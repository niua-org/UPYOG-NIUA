// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.validator.MDMSValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BookingValidatorTest {
    @Mock
    private MDMSValidator mdmsValidator;
    @Mock
    private BookingConfiguration config;

    private BookingValidator subject;

    @BeforeEach
    void setUp() {
        subject = new BookingValidator(mdmsValidator, config);
    }


    @Test
    void validateCreate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
