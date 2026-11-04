package org.egov.egf.master.domain.repository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class SubSchemeRepositoryTest {
    @ExtendWith(MockitoExtension.class)
    class UnitTests {
        @Test
        void classLoads() {
            assertThat(SubSchemeRepository.class).isNotNull();
        }
    }
}
