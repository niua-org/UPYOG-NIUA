package org.egov.ewst.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.ewst.config.EwasteConfiguration;
import org.egov.ewst.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteUtilTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private EwasteConfiguration configs;
    @Mock
    private ServiceRequestRepository restRepo;

    private EwasteUtil subject;

    @BeforeEach
    void setUp() {
        subject = new EwasteUtil(mapper, configs, restRepo);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
