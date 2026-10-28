package org.upyog.adv.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.web.models.ApplicantDetail;
import org.upyog.adv.web.models.BookingDetail;
import org.upyog.adv.web.models.BookingRequest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class BookingValidatorTest {

    @Mock
    private MDMSValidator mdmsValidator;
    @Mock
    private BookingConfiguration config;

    @InjectMocks
    private BookingValidator bookingValidator;

    @Test
    void validateCreate_whenCartDetailsMissing_throwsCustomException() {
        BookingRequest request = BookingRequest.builder()
                .bookingApplication(BookingDetail.builder()
                        .applicantDetail(ApplicantDetail.builder().applicantMobileNo("9999999999").build())
                        .cartDetails(Collections.emptyList())
                        .build())
                .build();

        assertThatThrownBy(() -> bookingValidator.validateCreate(request, null))
                .isInstanceOf(CustomException.class);
    }
}
