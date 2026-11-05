package org.upyog.adv.service.impl;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
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
import org.upyog.adv.web.models.AdvertisementSearchCriteria;
import org.upyog.adv.web.models.BookingDetail;
import org.upyog.adv.web.models.BookingRequest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private MdmsUtil mdmsUtil;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingValidator bookingValidator;
    @Mock private EnrichmentService enrichmentService;
    @Mock private DemandService demandService;
    @Mock private PaymentTimerService paymentTimerService;
    @Mock private ADVEncryptionService encryptionService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void createBooking_whenStateLevelTenant_throws() {
        BookingRequest request = BookingRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("u1").build()).build())
                .bookingApplication(BookingDetail.builder().tenantId("pb").build())
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("valid tenant");
    }

    @Test
    void getBookingCount_setsCountCallAndDelegatesToRepository() {
        AdvertisementSearchCriteria criteria = AdvertisementSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(bookingRepository.getBookingCount(criteria)).thenReturn(3);

        assertThat(bookingService.getBookingCount(criteria, RequestInfo.builder().build())).isEqualTo(3);
        assertThat(criteria.isCountCall()).isTrue();
    }

    @Test
    void getBookingDetails_whenRepositoryEmpty_returnsEmpty() {
        AdvertisementSearchCriteria criteria = AdvertisementSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(bookingRepository.getBookingDetails(criteria)).thenReturn(Collections.emptyList());

        assertThat(bookingService.getBookingDetails(criteria, RequestInfo.builder().build())).isEmpty();
    }
}
