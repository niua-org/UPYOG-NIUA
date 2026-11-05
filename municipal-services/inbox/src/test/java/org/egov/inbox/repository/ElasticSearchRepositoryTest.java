// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.inbox.repository.ElasticSearchQueryBuilder;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ElasticSearchRepositoryTest {
    @Mock
    private InboxConfiguration config;
    @Mock
    private ElasticSearchQueryBuilder queryBuilder;
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
    void elasticSearchApplications_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
