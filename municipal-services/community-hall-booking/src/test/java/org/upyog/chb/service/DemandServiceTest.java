package org.upyog.chb.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.repository.DemandRepository;
import org.upyog.chb.util.MdmsUtil;
import org.upyog.chb.validator.CommunityHallBookingValidator;
import org.upyog.chb.web.models.BookingSlotDetail;
import org.upyog.chb.web.models.CommunityHallDemandEstimationCriteria;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {

    @Mock private CommunityHallBookingConfiguration config;
    @Mock private CalculationService calculationService;
    @Mock private DemandRepository demandRepository;
    @Mock private CommunityHallBookingValidator bookingValidator;
    @Mock private MdmsUtil mdmsUtil;

    @InjectMocks
    private DemandService demandService;

    @Test
    void getDemand_whenStateTenant_throws() {
        BookingSlotDetail slot = BookingSlotDetail.builder().bookingDate(LocalDate.of(2026, 1, 1)).build();
        CommunityHallDemandEstimationCriteria criteria = CommunityHallDemandEstimationCriteria.builder()
                .tenantId("pb")
                .venueCode("hall1")
                .bookingSlotDetails(Collections.singletonList(slot))
                .requestInfo(RequestInfo.builder().build())
                .build();
        when(bookingValidator.isSameHallCode(criteria.getBookingSlotDetails())).thenReturn(true);

        assertThatThrownBy(() -> demandService.getDemand(criteria))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("valid tenant");
    }
}
