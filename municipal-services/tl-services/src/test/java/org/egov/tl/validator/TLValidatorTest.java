package org.egov.tl.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.TLRepository;
import org.egov.tl.service.UserService;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.egov.tl.util.TLConstants.businessService_TL;

@ExtendWith(MockitoExtension.class)
class TLValidatorTest {

    @Mock
    private TLRepository tlRepository;
    @Mock
    private TLConfiguration config;
    @Mock
    private PropertyValidator propertyValidator;
    @Mock
    private MDMSValidator mdmsValidator;
    @Mock
    private TradeUtil tradeUtil;
    @Mock
    private UserService userService;

    @InjectMocks
    private TLValidator tlValidator;

    @BeforeEach
    void stubAllowedBusinessServices() {
        ReflectionTestUtils.setField(tlValidator, "allowedBusinessService", "TL,BPAREG");
        ReflectionTestUtils.setField(tlValidator, "businessServiceTL", "TL");
    }

    @Test
    void validateBusinessService_whenPathNotAllowed_throws() {
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(TradeLicense.builder().businessService(businessService_TL).build()))
                .build();

        assertThatThrownBy(() -> tlValidator.validateBusinessService(request, "UNKNOWN"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("not allowed in this module");
    }

    @Test
    void validateBusinessService_whenLicenseServiceMismatch_throws() {
        TradeLicense license = TradeLicense.builder().businessService("BPAREG").build();
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(license))
                .build();

        assertThatThrownBy(() -> tlValidator.validateBusinessService(request, businessService_TL))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("not matching with the one sent in path variable");
    }

    @Test
    void validateBusinessService_whenConsistent_passes() {
        TradeLicense license = TradeLicense.builder().businessService(businessService_TL).build();
        TradeLicenseRequest request = TradeLicenseRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .licenses(List.of(license))
                .build();

        tlValidator.validateBusinessService(request, businessService_TL);
    }
}
