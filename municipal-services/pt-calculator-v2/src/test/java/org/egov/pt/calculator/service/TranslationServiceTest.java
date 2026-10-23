// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.calculator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pt.calculator.repository.Repository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private Repository repository;

    private TranslationService subject;

    @BeforeEach
    void setUp() {
        subject = new TranslationService(mapper, repository);
    }


    @Test
    void translate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
