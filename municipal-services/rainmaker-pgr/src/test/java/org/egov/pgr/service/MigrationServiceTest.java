// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.pgr.producer.PGRProducer;
import org.egov.pgr.service.GrievanceService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {
    @Mock
    private PGRProducer producer;
    @Mock
    private GrievanceService grievanceService;

    private MigrationService subject;

    @BeforeEach
    void setUp() {
        subject = new MigrationService(producer, grievanceService);
    }


    @Test
    void migrateData_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
