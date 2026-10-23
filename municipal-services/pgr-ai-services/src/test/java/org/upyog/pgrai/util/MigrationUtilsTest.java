// generated-by: generate_per_class_unit_tests.py
package org.upyog.pgrai.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.pgrai.config.PGRConfiguration;
import org.upyog.pgrai.repository.ServiceRequestRepository;
import org.upyog.pgrai.util.MDMSUtils;
import org.upyog.pgrai.util.UserUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MigrationUtilsTest {
    @Mock
    private UserUtils userUtils;
    @Mock
    private PGRConfiguration config;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository repository;
    @Mock
    private MDMSUtils mdmsUtils;

    private MigrationUtils subject;

    @BeforeEach
    void setUp() {
        subject = new MigrationUtils(userUtils, config, mapper, repository, mdmsUtils);
    }


    @Test
    void getIdtoUUIDMap_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
