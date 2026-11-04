package org.egov.reciept.consumer.config;

import org.egov.receipt.custom.exception.VoucherCustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesManagerTest {

    private PropertiesManager propertiesManager;

    @BeforeEach
    void setUp() {
        propertiesManager = new PropertiesManager();
        ReflectionTestUtils.setField(propertiesManager, "finCoeErpDomainName", "example.org");
        ReflectionTestUtils.setField(propertiesManager, "finCoeErpEnvName", "dev");
        ReflectionTestUtils.setField(propertiesManager, "httpProtocol", "https");
    }

    @Test
    void getErpURLBytenantId_buildsTenantHost() throws VoucherCustomException {
        String url = propertiesManager.getErpURLBytenantId("pb.amritsar");

        assertThat(url).isEqualTo("https://amritsar-dev.example.org/");
    }

    @Test
    void getErpURLBytenantId_localEnvironment_returnsFixedUrl() throws VoucherCustomException {
        ReflectionTestUtils.setField(propertiesManager, "finCoeErpEnvName", "local");

        String url = propertiesManager.getErpURLBytenantId("pb.amritsar");

        assertThat(url).isEqualTo("http://jalandhar.lgpunjab.com:8080/");
    }

    @Test
    void getErpURLBytenantId_invalidTenant_throws() {
        assertThatThrownBy(() -> propertiesManager.getErpURLBytenantId("invalid"))
                .isInstanceOf(VoucherCustomException.class);
    }
}
