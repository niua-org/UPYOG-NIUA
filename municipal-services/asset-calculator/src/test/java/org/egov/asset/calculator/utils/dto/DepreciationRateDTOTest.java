package org.egov.asset.calculator.utils.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DepreciationRateDTOTest {

    @Test
    void setRate_whenRoundTrip_retainsValue() {
        DepreciationRateDTO target = new DepreciationRateDTO();
        target.setRate(java.math.BigDecimal.ONE);
        assertThat(target.getRate()).isEqualTo(java.math.BigDecimal.ONE);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new DepreciationRateDTO()).isNotNull();
    }
}
