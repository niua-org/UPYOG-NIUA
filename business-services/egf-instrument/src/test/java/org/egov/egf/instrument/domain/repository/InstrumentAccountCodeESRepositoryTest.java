package org.egov.egf.instrument.domain.repository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class InstrumentAccountCodeESRepositoryTest {
    @ExtendWith(MockitoExtension.class)
    class UnitTests {
        @Test
        void classLoads() {
            assertThat(InstrumentAccountCodeESRepository.class).isNotNull();
        }
    }
}
