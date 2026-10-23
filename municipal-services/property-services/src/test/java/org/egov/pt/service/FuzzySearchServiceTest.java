// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.repository.ElasticSearchRepository;
import org.egov.pt.repository.PropertyRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FuzzySearchServiceTest {
    @Mock
    private ElasticSearchRepository elasticSearchRepository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private PropertyRepository repository;
    @Mock
    private PropertyConfiguration config;

    private FuzzySearchService subject;

    @BeforeEach
    void setUp() {
        subject = new FuzzySearchService(elasticSearchRepository, mapper, repository, config);
    }


    @Test
    void getProperties_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
