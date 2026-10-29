// generated-by: generate_per_class_unit_tests.py
package org.bel.birthdeath.common.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bel.birthdeath.common.repository.CommonRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommonServiceTest {
    @Mock
    private CommonRepository repository;

    private CommonService subject;

    @BeforeEach
    void setUp() {
        subject = new CommonService(repository);
    }


    @Test
    void search_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
