// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.repository.BookingRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {
    @Mock
    private BookingRepository bookingRepo;

    private SchedulerService subject;

    @BeforeEach
    void setUp() {
        subject = new SchedulerService(bookingRepo);
    }


    @Test
    void cleanupExpiredEntries_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
