// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.calculator.repository.CustomDepreciationRepository;
import org.egov.asset.calculator.repository.DepreciationDetailRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ProcessDepreciationTest {
    @Mock
    private DepreciationDetailRepository depreciationDetailRepository;
    @Mock
    private CustomDepreciationRepository customDepreciationRepository;

    private ProcessDepreciation subject;

    @BeforeEach
    void setUp() {
        subject = new ProcessDepreciation(depreciationDetailRepository, customDepreciationRepository);
    }


    @Test
    void executeBulkDepreciationProcedure_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
