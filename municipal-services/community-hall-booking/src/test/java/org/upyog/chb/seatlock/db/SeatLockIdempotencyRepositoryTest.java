// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.seatlock.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SeatLockIdempotencyRepositoryTest {
    @Mock
    private JdbcTemplate jdbc;

    private SeatLockIdempotencyRepository subject;

    @BeforeEach
    void setUp() {
        subject = new SeatLockIdempotencyRepository(jdbc);
    }


    @Test
    void findOutcome_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
