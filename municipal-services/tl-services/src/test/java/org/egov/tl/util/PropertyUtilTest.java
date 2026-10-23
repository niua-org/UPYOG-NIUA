// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.ServiceRequestRepository;
import org.egov.tl.util.NotificationUtil;
import org.egov.tl.util.TradeUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PropertyUtilTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private NotificationUtil util;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private TLConfiguration config;
    @Mock
    private TradeUtil tradeUtil;

    private PropertyUtil subject;

    @BeforeEach
    void setUp() {
        subject = new PropertyUtil(serviceRequestRepository, util, mapper, config, tradeUtil);
    }


    @Test
    void getPropertySearchMsg_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
