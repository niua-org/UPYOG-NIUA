package org.egov.gis.service;

import org.egov.gis.interfaces.MunicipalServiceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryTest {

    @Mock
    private MunicipalServiceAdapter propertyAdapter;

    @Mock
    private MunicipalServiceAdapter assetAdapter;

    @Test
    void getAdapter_knownServiceId_returnsMatchingAdapter() {
        when(propertyAdapter.getBusinessService()).thenReturn("PT");
        ServiceRegistry registry = registryWith(propertyAdapter);

        assertThat(registry.getAdapter("PT")).isSameAs(propertyAdapter);
        assertThat(registry.hasAdapter("PT")).isTrue();
    }

    @Test
    void getAdapter_unknownServiceId_throwsIllegalArgumentException() {
        when(propertyAdapter.getBusinessService()).thenReturn("PT");
        ServiceRegistry registry = registryWith(propertyAdapter);

        assertThatThrownBy(() -> registry.getAdapter("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No adapter found for business service: UNKNOWN");
        assertThat(registry.hasAdapter("UNKNOWN")).isFalse();
    }

    @Test
    void getAvailableBusinessServices_knownAdaptersRegistered_returnsServiceIds() {
        when(propertyAdapter.getBusinessService()).thenReturn("PT");
        when(assetAdapter.getBusinessService()).thenReturn("ASSET");
        ServiceRegistry registry = new ServiceRegistry(Arrays.asList(propertyAdapter, assetAdapter));
        registry.initializeAdapters();

        assertThat(registry.getAvailableBusinessServices()).containsExactlyInAnyOrder("PT", "ASSET");
    }

    @Test
    void getAvailableBusinessServices_emptyAdapterList_returnsEmptyList() {
        ServiceRegistry registry = new ServiceRegistry(Collections.<MunicipalServiceAdapter>emptyList());
        registry.initializeAdapters();

        assertThat(registry.getAvailableBusinessServices()).isEmpty();
        assertThat(registry.hasAdapter("PT")).isFalse();
    }

    @Test
    void getAdapter_nullServiceId_throwsIllegalArgumentException() {
        when(propertyAdapter.getBusinessService()).thenReturn("PT");
        ServiceRegistry registry = registryWith(propertyAdapter);

        assertThatThrownBy(() -> registry.getAdapter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No adapter found for business service: null");
    }

    @Test
    void initializeAdapters_nullAdapterList_throwsNullPointerException() {
        ServiceRegistry registry = new ServiceRegistry(null);

        assertThatThrownBy(registry::initializeAdapters)
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getAdapter_duplicateServiceId_keepsLastRegisteredAdapter() {
        when(propertyAdapter.getBusinessService()).thenReturn("PT");
        when(assetAdapter.getBusinessService()).thenReturn("PT");
        ServiceRegistry registry = new ServiceRegistry(Arrays.asList(propertyAdapter, assetAdapter));
        registry.initializeAdapters();

        assertThat(registry.getAdapter("PT")).isSameAs(assetAdapter);
        assertThat(registry.getAvailableBusinessServices()).containsExactly("PT");
    }

    private static ServiceRegistry registryWith(MunicipalServiceAdapter adapter) {
        ServiceRegistry registry = new ServiceRegistry(Collections.singletonList(adapter));
        registry.initializeAdapters();
        return registry;
    }
}
