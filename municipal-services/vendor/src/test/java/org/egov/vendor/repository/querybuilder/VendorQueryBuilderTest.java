package org.egov.vendor.repository.querybuilder;

import org.egov.vendor.web.model.VendorSearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VendorQueryBuilderTest {

    private final VendorQueryBuilder queryBuilder = new VendorQueryBuilder();

    @Test
    void getDriverSearchQuery_returnsParameterizedLookup() {
        assertThat(queryBuilder.getDriverSearchQuery()).contains("eg_vendor_driver");
    }

    @Test
    void vendorsForVehicles_withVehicleIds_buildsInClause() {
        VendorSearchCriteria criteria = VendorSearchCriteria.builder()
                .vehicleIds(List.of("veh-1", "veh-2"))
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.vendorsForVehicles(criteria, params);

        assertThat(sql).contains("eg_vendor_vehicle");
        assertThat(params).contains("veh-1", "veh-2");
    }
}
