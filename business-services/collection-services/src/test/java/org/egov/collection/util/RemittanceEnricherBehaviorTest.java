package org.egov.collection.util;

import org.egov.collection.model.enums.RemittanceStatus;
import org.egov.collection.web.contract.Remittance;
import org.egov.collection.web.contract.RemittanceDetail;
import org.egov.collection.web.contract.RemittanceInstrument;
import org.egov.collection.web.contract.RemittanceReceipt;
import org.egov.collection.web.contract.RemittanceRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RemittanceEnricherBehaviorTest {

    private final RemittanceEnricher enricher = new RemittanceEnricher();

    @Test
    void enrichRemittancePreValidate_assignsIdsAndAudit() {
        RemittanceDetail detail = new RemittanceDetail();
        RemittanceInstrument instrument = new RemittanceInstrument();
        RemittanceReceipt receipt = new RemittanceReceipt();
        Set<RemittanceDetail> details = new HashSet<>();
        details.add(detail);
        Set<RemittanceInstrument> instruments = new HashSet<>();
        instruments.add(instrument);
        Set<RemittanceReceipt> receipts = new HashSet<>();
        receipts.add(receipt);
        Remittance remittance = Remittance.builder()
                .tenantId("pb")
                .remittanceDetails(details)
                .remittanceInstruments(instruments)
                .remittanceReceipts(receipts)
                .build();

        User user = new User();
        user.setId(99L);
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(user);

        RemittanceRequest request = RemittanceRequest.builder()
                .remittances(new java.util.ArrayList<>(java.util.List.of(remittance)))
                .requestInfo(requestInfo)
                .build();

        enricher.enrichRemittancePreValidate(request);

        assertThat(remittance.getId()).isNotBlank();
        assertThat(remittance.getStatus()).isEqualTo(RemittanceStatus.APPROVED.name());
        assertThat(detail.getId()).isNotBlank();
        assertThat(instrument.getRemittance()).isEqualTo(remittance.getId());
        assertThat(receipt.getTenantId()).isEqualTo("pb");
    }
}
