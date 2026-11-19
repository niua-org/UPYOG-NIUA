package org.egov.vehicle.service;

import org.egov.tracer.model.CustomException;
import org.egov.vehicle.repository.VehicleRepository;
import org.egov.vehicle.trip.web.model.VehicleTripSearchCriteria;
import org.egov.vehicle.web.model.VehicleSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class VehicleInboxServiceTest {

    @Mock private VehicleRepository vehicleRepository;
    @InjectMocks private VehicleInboxService vehicleInboxService;

    @Test
    void fetchStatusCount_whenStateTenant_throws() {
        VehicleSearchCriteria criteria = VehicleSearchCriteria.builder().tenantId("pb").build();

        assertThatThrownBy(() -> vehicleInboxService.fetchStatusCount(criteria))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Tenant is not available");
    }

    @Test
    void fetchTripDetails_whenStateTenant_throws() {
        VehicleTripSearchCriteria criteria = VehicleTripSearchCriteria.builder().tenantId("pb").build();

        assertThatThrownBy(() -> vehicleInboxService.fetchTripDetails(criteria))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Tenant is not available");
    }
}
