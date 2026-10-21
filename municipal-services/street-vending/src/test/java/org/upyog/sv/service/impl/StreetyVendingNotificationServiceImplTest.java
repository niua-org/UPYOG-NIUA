// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.sv.config.StreetVendingConfiguration;
import org.upyog.sv.service.StreetVendingEncryptionService;
import org.upyog.sv.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StreetyVendingNotificationServiceImplTest {
    @Mock
    private StreetVendingConfiguration config;
    @Mock
    private NotificationUtil util;
    @Mock
    private StreetVendingEncryptionService decrypt;

    private StreetyVendingNotificationServiceImpl subject;

    @BeforeEach
    void setUp() {
        subject = new StreetyVendingNotificationServiceImpl(config, util, decrypt);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
