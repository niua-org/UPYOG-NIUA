package org.egov.dx.adapter;

import org.egov.dx.adapter.vendor.gst.impl.IrisGstGatewayAdapter;
import org.egov.dx.adapter.vendor.gst.impl.MockGstGatewayAdapter;
import org.egov.dx.adapter.vendor.pan.impl.ItdPanGatewayAdapter;
import org.egov.dx.adapter.vendor.pan.impl.MockPanGatewayAdapter;
import org.egov.dx.adapter.vendor.tan.impl.ItdTanGatewayAdapter;
import org.egov.dx.adapter.vendor.tan.impl.MockTanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Context test verifying conditional bean gating for PAN, TAN, and GST gateway adapters.
 * Ensures mock beans are NEVER loaded by default or when mock is disabled, and production adapters
 * load exclusively when mock.enabled=false.
 */
class VerificationBeanGatingContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(Configurations.class);

    /**
     * Tests Pan Adapter Gating under the when mock enabled_loads mock adapter only scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testPanAdapterGating_WhenMockEnabled_LoadsMockAdapterOnly() {
        contextRunner
                .withUserConfiguration(MockPanGatewayAdapter.class, ItdPanGatewayAdapter.class)
                .withPropertyValues(
                        "pan.verification.mock.enabled=true",
                        "pan.verification.mock.records.path=mock/pan-mock-records.json"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MockPanGatewayAdapter.class);
                    assertThat(context).doesNotHaveBean(ItdPanGatewayAdapter.class);
                });
    }

    /**
     * Tests Pan Adapter Gating under the when mock disabled_loads live adapter only scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testPanAdapterGating_WhenMockDisabled_LoadsLiveAdapterOnly() {
        contextRunner
                .withUserConfiguration(MockPanGatewayAdapter.class, ItdPanGatewayAdapter.class)
                .withPropertyValues(
                        "pan.verification.mock.enabled=false",
                        "pan.verification.connect.timeout=5000",
                        "pan.verification.read.timeout=10000",
                        "pan.verification.base.url=https://uatservices.incometax.gov.in/services/auth/v0.1/"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ItdPanGatewayAdapter.class);
                    assertThat(context).doesNotHaveBean(MockPanGatewayAdapter.class);
                });
    }

    /**
     * Tests Pan Adapter Gating under the when mock property absent_mock does not load scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testPanAdapterGating_WhenMockPropertyAbsent_MockDoesNotLoad() {
        contextRunner
                .withUserConfiguration(MockPanGatewayAdapter.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MockPanGatewayAdapter.class);
                });
    }

    /**
     * Tests Tan Adapter Gating under the when mock enabled_loads mock adapter only scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testTanAdapterGating_WhenMockEnabled_LoadsMockAdapterOnly() {
        contextRunner
                .withUserConfiguration(MockTanGatewayAdapter.class, ItdTanGatewayAdapter.class)
                .withPropertyValues(
                        "tan.verification.mock.enabled=true",
                        "tan.verification.mock.records.path=mock/tan-mock-records.json"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MockTanGatewayAdapter.class);
                    assertThat(context).doesNotHaveBean(ItdTanGatewayAdapter.class);
                });
    }

    /**
     * Tests Tan Adapter Gating under the when mock disabled_loads live adapter only scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testTanAdapterGating_WhenMockDisabled_LoadsLiveAdapterOnly() {
        contextRunner
                .withUserConfiguration(MockTanGatewayAdapter.class, ItdTanGatewayAdapter.class)
                .withPropertyValues(
                        "tan.verification.mock.enabled=false",
                        "tan.verification.connect.timeout=5000",
                        "tan.verification.read.timeout=10000",
                        "tan.verification.base.url=https://uatservices.incometax.gov.in/services/auth/v0.1/"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ItdTanGatewayAdapter.class);
                    assertThat(context).doesNotHaveBean(MockTanGatewayAdapter.class);
                });
    }

    /**
     * Tests Tan Adapter Gating under the when mock property absent_mock does not load scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testTanAdapterGating_WhenMockPropertyAbsent_MockDoesNotLoad() {
        contextRunner
                .withUserConfiguration(MockTanGatewayAdapter.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MockTanGatewayAdapter.class);
                });
    }

    /**
     * Tests Gst Adapter Gating under the when mock enabled_loads mock adapter only scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testGstAdapterGating_WhenMockEnabled_LoadsMockAdapterOnly() {
        contextRunner
                .withUserConfiguration(MockGstGatewayAdapter.class, IrisGstGatewayAdapter.class)
                .withPropertyValues(
                        "gst.verification.mock.enabled=true",
                        "gst.verification.mock.records.path=mock/gst-mock-records.json"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MockGstGatewayAdapter.class);
                    assertThat(context).doesNotHaveBean(IrisGstGatewayAdapter.class);
                });
    }

    /**
     * Tests Gst Adapter Gating under the when mock disabled_loads live adapter only scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testGstAdapterGating_WhenMockDisabled_LoadsLiveAdapterOnly() {
        contextRunner
                .withUserConfiguration(MockGstGatewayAdapter.class, IrisGstGatewayAdapter.class)
                .withPropertyValues(
                        "gst.verification.mock.enabled=false",
                        "gst.verification.connect.timeout=5000",
                        "gst.verification.read.timeout=10000",
                        "gst.verification.base.url=https://taxpayer.irisgst.com/"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(IrisGstGatewayAdapter.class);
                    assertThat(context).doesNotHaveBean(MockGstGatewayAdapter.class);
                });
    }

    /**
     * Tests Gst Adapter Gating under the when mock property absent_mock does not load scenario.
     *
     * Verifies the expected response and model assertions.
     */
    @Test
    void testGstAdapterGating_WhenMockPropertyAbsent_MockDoesNotLoad() {
        contextRunner
                .withUserConfiguration(MockGstGatewayAdapter.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MockGstGatewayAdapter.class);
                });
    }
}
