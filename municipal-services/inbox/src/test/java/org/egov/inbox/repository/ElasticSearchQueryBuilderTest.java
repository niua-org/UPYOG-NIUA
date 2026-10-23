// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.encryption.EncryptionService;
import org.egov.inbox.config.InboxConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ElasticSearchQueryBuilderTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private InboxConfiguration config;
    @Mock
    private EncryptionService encryptionService;

    private ElasticSearchQueryBuilder subject;

    @BeforeEach
    void setUp() {
        subject = new ElasticSearchQueryBuilder(mapper, config, encryptionService);
    }


    @Test
    void getSearchQuery_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
