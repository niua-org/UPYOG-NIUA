// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @InjectMocks
    private CalculationService subject;

    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }
}
