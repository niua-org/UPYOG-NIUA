// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.repository.TLRepository;
import org.egov.tl.util.TradeUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private TLConfiguration config;
    @Mock
    private TradeUtil tradeUtil;
    @Mock
    private TLRepository repository;

    private UserService subject;

    @BeforeEach
    void setUp() {
        subject = new UserService(mapper, serviceRequestRepository, config, tradeUtil, repository);
    }


    @Test
    void createUser_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
