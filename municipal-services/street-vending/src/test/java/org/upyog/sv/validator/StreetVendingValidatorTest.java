// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.sv.config.StreetVendingConfiguration;
import org.upyog.sv.repository.StreetVendingRepository;
import org.upyog.sv.validator.MDMSValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StreetVendingValidatorTest {
    @Mock
    private MDMSValidator mdmsValidator;
    @Mock
    private StreetVendingConfiguration config;
    @Mock
    private StreetVendingRepository repository;

    private StreetVendingValidator subject;

    @BeforeEach
    void setUp() {
        subject = new StreetVendingValidator(mdmsValidator, config, repository);
    }


    @Test
    void validateCreate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
