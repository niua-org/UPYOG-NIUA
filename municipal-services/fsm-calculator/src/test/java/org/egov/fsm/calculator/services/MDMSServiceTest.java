// generated-by: generate_per_class_unit_tests.py
package org.egov.fsm.calculator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MDMSServiceTest {


    @InjectMocks
    private MDMSService subject;


    @Test
    void mDMSCall_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
