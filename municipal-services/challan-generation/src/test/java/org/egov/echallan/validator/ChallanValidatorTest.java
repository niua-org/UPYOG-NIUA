// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChallanValidatorTest {
    @Mock
    private ChallanConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private ChallanValidator subject;

    @BeforeEach
    void setUp() {
        subject = new ChallanValidator(config, serviceRequestRepository);
    }


    @Test
    void validateFields_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
