package org.upyog.smoke.finance;

import org.junit.jupiter.api.Tag;
import org.upyog.smoke.support.ServiceSmokeTestTemplate;

@Tag("smoke")
public class FinanceCollectionsVoucherConsumerSmokeTest extends ServiceSmokeTestTemplate {

    public FinanceCollectionsVoucherConsumerSmokeTest() {
        super("finance-collections-voucher-consumer");
    }
}
