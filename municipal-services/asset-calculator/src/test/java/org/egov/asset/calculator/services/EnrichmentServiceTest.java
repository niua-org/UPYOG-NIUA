// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.calculator.utils.AssetCalculatorUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private AssetCalculatorUtil assetCalculatorUtil;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(assetCalculatorUtil);
    }


    @Test
    void enrichOtherOperations_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
