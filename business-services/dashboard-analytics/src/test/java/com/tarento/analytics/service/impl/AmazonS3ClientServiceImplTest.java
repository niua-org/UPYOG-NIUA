package com.tarento.analytics.service.impl;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class AmazonS3ClientServiceImplTest {
    @ExtendWith(MockitoExtension.class)
    class UnitTests {
        @Test
        void classLoads() {
            assertThat(AmazonS3ClientServiceImpl.class).isNotNull();
        }
    }
}
