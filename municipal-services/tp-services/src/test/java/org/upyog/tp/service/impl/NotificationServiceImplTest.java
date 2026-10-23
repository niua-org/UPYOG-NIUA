// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.tp.config.TreePruningConfiguration;
import org.upyog.tp.util.NotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
    @Mock
    private TreePruningConfiguration config;
    @Mock
    private NotificationUtil util;

    private NotificationServiceImpl subject;

    @BeforeEach
    void setUp() {
        subject = new NotificationServiceImpl(config, util);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
