package org.upyog.cdwm.calculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CNDApplicationDetailTest {

    @Test
    void setTotalWasteQuantity_whenRoundTrip_retainsValue() {
        CNDApplicationDetail target = new CNDApplicationDetail();
        target.setTotalWasteQuantity(java.math.BigDecimal.ONE);
        assertThat(target.getTotalWasteQuantity()).isEqualTo(java.math.BigDecimal.ONE);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new CNDApplicationDetail()).isNotNull();
    }
}
