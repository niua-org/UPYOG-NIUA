package org.egov.waterconnection.validator;

import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.web.models.SearchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaterConnectionValidatorTest {

    @InjectMocks
    private WaterConnectionValidator validator;

    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;

    @Test
    @DisplayName("validateSearch_whenCentralInstanceAndTenantMissing_throwsCustomException")
    void validateSearch_whenCentralInstanceAndTenantMissing_throwsCustomException() {
        when(centralInstanceUtil.getIsEnvironmentCentralInstance()).thenReturn(true);
        SearchCriteria criteria = SearchCriteria.builder().build();
        assertThatThrownBy(() -> validator.validateSearch(criteria))
                .isInstanceOf(CustomException.class);
    }
}
