// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.producer.Producer;
import org.egov.tl.repository.builder.TLQueryBuilder;
import org.egov.tl.repository.rowmapper.TLRowMapper;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.workflow.WorkflowService;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TLRepositoryTest {
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TLQueryBuilder queryBuilder;
    @Mock
    private TLRowMapper rowMapper;
    @Mock
    private Producer producer;
    @Mock
    private TLConfiguration config;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private TradeUtil utils;
    @Mock
    private MultiStateInstanceUtil multiStateInstanceUtil;

    private TLRepository subject;

    @BeforeEach
    void setUp() {
        subject = new TLRepository(jdbcTemplate, queryBuilder, rowMapper, producer, config, workflowService, utils, multiStateInstanceUtil);
    }


    @Test
    void getLicenses_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
