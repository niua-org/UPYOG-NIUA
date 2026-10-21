// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.validator.MDMSValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommunityHallBookingValidatorTest {
    @Mock
    private MDMSValidator mdmsValidator;
    @Mock
    private CommunityHallBookingConfiguration config;

    private CommunityHallBookingValidator subject;

    @BeforeEach
    void setUp() {
        subject = new CommunityHallBookingValidator(mdmsValidator, config);
    }


    @Test
    void validateCreate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
