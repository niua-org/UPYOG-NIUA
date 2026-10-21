// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.trip.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.vehicle.trip.workflow.WorkflowService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ActionValidatorTest {
    @Mock
    private WorkflowService workflowService;

    private ActionValidator subject;

    @BeforeEach
    void setUp() {
        subject = new ActionValidator(workflowService);
    }


    @Test
    void validateUpdateRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
