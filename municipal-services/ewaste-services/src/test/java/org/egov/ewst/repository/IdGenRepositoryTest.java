// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IdGenRepositoryTest {
    @Mock
    private RestTemplate restTemplate;

    private IdGenRepository subject;

    @BeforeEach
    void setUp() {
        subject = new IdGenRepository(restTemplate);
    }


    @Test
    void getId_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
