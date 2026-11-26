// generated-by: generate_per_class_unit_tests.py
package org.bel.birthdeath.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bel.birthdeath.common.services.CommonService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommonRepositoryTest {
    @Mock
    private CommonService commonService;

    private CommonRepository subject;

    @BeforeEach
    void setUp() {
        subject = new CommonRepository(commonService);
    }


    @Test
    void getHospitalDtls_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
