// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.repository.BookingRepository;
import org.upyog.adv.service.ADVEncryptionService;
import org.upyog.adv.service.DemandService;
import org.upyog.adv.service.EnrichmentService;
import org.upyog.adv.service.PaymentTimerService;
import org.upyog.adv.util.MdmsUtil;
import org.upyog.adv.validator.BookingValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock
    private MdmsUtil mdmsUtil;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingValidator bookingValidator;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private DemandService demandService;
    @Mock
    private PaymentTimerService paymentTimerService;
    @Mock
    private ADVEncryptionService encryptionService;

    private BookingServiceImpl subject;

    @BeforeEach
    void setUp() {
        subject = new BookingServiceImpl(mdmsUtil, bookingRepository, bookingValidator, enrichmentService, demandService, paymentTimerService, encryptionService);
    }


    @Test
    void createBooking_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
