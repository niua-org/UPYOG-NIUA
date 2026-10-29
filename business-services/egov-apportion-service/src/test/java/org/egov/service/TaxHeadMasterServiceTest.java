package org.egov.service;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxHeadMasterServiceTest {

    private final TaxHeadMasterService service = new TaxHeadMasterService();

    private static final Object MDMS = Map.of(
            "MdmsRes", Map.of(
                    "BillingService", Map.of(
                            "TaxHeadMaster", List.of(
                                    Map.of("code", "ADV_TAX", "service", "WS", "category", "ADVANCE_COLLECTION", "order", "99"),
                                    Map.of("code", "TAX1", "service", "WS", "order", "1")
                            ),
                            "BusinessService", List.of(
                                    Map.of("code", "WS", "isAdvanceAllowed", true)
                            )
                    )
            )
    );

    @Test
    void getAdvanceTaxHead_returnsFirstMatch() {
        assertThat(service.getAdvanceTaxHead("WS", MDMS)).isEqualTo("ADV_TAX");
    }

    @Test
    void getCodeToOrderMap_buildsPriorityMap() {
        assertThat(service.getCodeToOrderMap("WS", MDMS)).containsEntry("TAX1", 1);
    }

    @Test
    void isAdvanceAllowed_returnsFlag() {
        assertThat(service.isAdvanceAllowed("WS", MDMS)).isTrue();
    }

    @Test
    void getAdvanceTaxHead_whenMissing_throws() {
        assertThatThrownBy(() -> service.getAdvanceTaxHead("UNKNOWN", MDMS))
                .isInstanceOf(CustomException.class);
    }
}
