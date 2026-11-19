package org.egov.tl.service;

import org.egov.tl.web.models.Difference;
import org.egov.tl.web.models.TradeLicense;
import org.egov.tl.web.models.TradeLicenseRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiffServiceTest {

    private final DiffService diffService = new DiffService();

    @Test
    void getDifference_whenTradeNameChanged_recordsFieldChange() {
        TradeLicense fromDb = TradeLicense.builder().id("lic-1").tradeName("Old Shop").build();
        TradeLicense fromUpdate = TradeLicense.builder().id("lic-1").tradeName("New Shop").build();
        TradeLicenseRequest request = TradeLicenseRequest.builder().licenses(List.of(fromUpdate)).build();

        Map<String, Difference> diffMap = diffService.getDifference(request, List.of(fromDb));

        assertThat(diffMap).containsKey("lic-1");
        assertThat(diffMap.get("lic-1").getFieldsChanged()).contains("tradeName");
    }

    @Test
    void getDifference_whenNoChanges_returnsEmptyFieldList() {
        TradeLicense license = TradeLicense.builder().id("lic-2").tradeName("Same").build();
        TradeLicenseRequest request = TradeLicenseRequest.builder().licenses(List.of(license)).build();

        Map<String, Difference> diffMap = diffService.getDifference(request, List.of(license));

        assertThat(diffMap.get("lic-2").getFieldsChanged()).isEmpty();
    }
}
