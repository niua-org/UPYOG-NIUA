// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.pt.service.PropertyService;
import org.egov.pt.util.UnmaskingUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssessmentUtilsTest {
    @Mock
    private PropertyService propertyService;
    @Mock
    private UnmaskingUtil unmaskingUtil;

    private AssessmentUtils subject;

    @BeforeEach
    void setUp() {
        subject = new AssessmentUtils(propertyService, unmaskingUtil);
    }


    @Test
    void getPropertyForAssessment_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
