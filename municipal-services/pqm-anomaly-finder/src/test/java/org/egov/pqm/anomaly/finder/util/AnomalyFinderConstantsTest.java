package org.egov.pqm.anomaly.finder.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyFinderConstantsTest {

    @Test
    void USREVENTS_EVENT_TYPE_whenRead_isNotBlank() {
        assertThat(AnomalyFinderConstants.USREVENTS_EVENT_TYPE).isNotBlank();
    }

}
