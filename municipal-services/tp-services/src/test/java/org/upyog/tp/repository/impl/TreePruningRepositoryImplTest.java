// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.upyog.tp.config.TreePruningConfiguration;
import org.upyog.tp.kafka.Producer;
import org.upyog.tp.repository.querybuilder.TreePruningQueryBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TreePruningRepositoryImplTest {
    @Mock
    private Producer producer;
    @Mock
    private TreePruningQueryBuilder queryBuilder;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TreePruningConfiguration treePruningConfiguration;

    private TreePruningRepositoryImpl subject;

    @BeforeEach
    void setUp() {
        subject = new TreePruningRepositoryImpl(producer, queryBuilder, jdbcTemplate, treePruningConfiguration);
    }


    @Test
    void saveTreePruningBooking_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
