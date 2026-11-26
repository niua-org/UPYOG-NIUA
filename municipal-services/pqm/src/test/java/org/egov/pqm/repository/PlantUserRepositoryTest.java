// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.pqm.config.ServiceConfiguration;
import org.egov.pqm.pqmProducer.PqmProducer;
import org.egov.pqm.repository.querybuilder.PlantUserQueryBuilder;
import org.egov.pqm.repository.rowmapper.PlantUserRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PlantUserRepositoryTest {
    @Mock
    private PlantUserQueryBuilder plantUserQueryBuilder;
    @Mock
    private PlantUserRowMapper plantUserRowMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PqmProducer pqmProducer;
    @Mock
    private ServiceConfiguration serviceConfig;

    private PlantUserRepository subject;

    @BeforeEach
    void setUp() {
        subject = new PlantUserRepository(plantUserQueryBuilder, plantUserRowMapper, jdbcTemplate, pqmProducer, serviceConfig);
    }


    @Test
    void save_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
