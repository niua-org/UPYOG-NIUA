package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VendorRoleTest {

    @Test
    void values_containsExpectedVendorRoles() {
        assertThat(VendorRole.values()).containsExactly(
                VendorRole.FSM_DSO,
                VendorRole.WT_VENDOR,
                VendorRole.MT_VENDOR,
                VendorRole.CND_VENDOR,
                VendorRole.CND_FACILITY_CEMP
        );
    }

    @Test
    void valueOf_resolvesKnownRole() {
        assertThat(VendorRole.valueOf("FSM_DSO")).isEqualTo(VendorRole.FSM_DSO);
    }
}
