// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.util.TradeUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MDMSValidatorTest {
    @Mock
    private ServiceRequestRepository requestRepository;
    @Mock
    private TradeUtil util;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private MDMSValidator subject;

    @BeforeEach
    void setUp() {
        subject = new MDMSValidator(requestRepository, util, serviceRequestRepository);
    }


    @Test
    void validateMdmsData_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
