// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.repository.builder.FuzzySearchQueryBuilder;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ElasticSearchRepositoryTest {
    @Mock
    private PropertyConfiguration config;
    @Mock
    private FuzzySearchQueryBuilder queryBuilder;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper mapper;

    private ElasticSearchRepository subject;

    @BeforeEach
    void setUp() {
        subject = new ElasticSearchRepository(config, queryBuilder, restTemplate, mapper);
    }


    @Test
    void fuzzySearchProperties_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
